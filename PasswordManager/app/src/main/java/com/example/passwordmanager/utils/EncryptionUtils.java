package com.example.passwordmanager.utils;

import android.os.Build;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;
import android.util.Log;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.SecureRandom;
import java.security.UnrecoverableEntryException;
import java.security.cert.CertificateException;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

public class EncryptionUtils {

    private static final String TAG = "EncryptionUtils";
    private static final String ANDROID_KEYSTORE_PROVIDER = "AndroidKeyStore";
    // Đổi tên KEY_ALIAS một chút để đảm bảo khóa mới được tạo với cấu hình đúng
    // nếu khóa cũ có thể đã được tạo với cấu hình khác.
    private static final String KEY_ALIAS = "PasswordManagerMasterKey_v2_AES_GCM";
    private static final String AES_MODE_GCM = KeyProperties.KEY_ALGORITHM_AES + "/" +
            KeyProperties.BLOCK_MODE_GCM + "/" +
            KeyProperties.ENCRYPTION_PADDING_NONE;
    private static final int GCM_IV_LENGTH = 12; // bytes, recommended for GCM
    private static final int GCM_TAG_LENGTH = 128; // bits

    private KeyStore keyStore;

    public EncryptionUtils() {
        Log.d(TAG, "Khởi tạo EncryptionUtils...");
        try {
            keyStore = KeyStore.getInstance(ANDROID_KEYSTORE_PROVIDER);
            keyStore.load(null);
            if (!keyStore.containsAlias(KEY_ALIAS)) {
                Log.i(TAG, "Khóa với alias '" + KEY_ALIAS + "' không tồn tại. Đang tạo khóa mới...");
                generateSecretKey();
            } else {
                Log.i(TAG, "Đã tìm thấy khóa với alias: " + KEY_ALIAS);
            }
        } catch (KeyStoreException | CertificateException | NoSuchAlgorithmException | IOException e) {
            Log.e(TAG, "Lỗi nghiêm trọng khi khởi tạo Keystore hoặc tải khóa!", e);
            throw new RuntimeException("Không thể khởi tạo Keystore cho mã hóa. Xem Logcat với TAG '" + TAG + "'.", e);
        }
        Log.d(TAG, "EncryptionUtils đã được khởi tạo thành công.");
    }

    private void generateSecretKey() {
        Log.d(TAG, "Bắt đầu tạo khóa bí mật mới với alias: " + KEY_ALIAS);
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                KeyGenerator keyGenerator = KeyGenerator.getInstance(
                        KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE_PROVIDER);

                KeyGenParameterSpec.Builder builder = new KeyGenParameterSpec.Builder(
                        KEY_ALIAS,
                        KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                        .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                        .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                        .setKeySize(256)
                        // Quan trọng: Khi true, Keystore tự tạo IV. Không được truyền IV khi encrypt.
                        .setRandomizedEncryptionRequired(true);

                keyGenerator.init(builder.build());
                keyGenerator.generateKey();
                Log.i(TAG, "Khóa AES/GCM mới đã được tạo thành công trong Keystore với alias: " + KEY_ALIAS);
            } else {
                Log.e(TAG, "Android Keystore cho AES/GCM yêu cầu API level 23 (Android M) trở lên.");
                throw new RuntimeException("Phiên bản Android không hỗ trợ Keystore cho AES/GCM.");
            }
        } catch (NoSuchAlgorithmException | NoSuchProviderException | InvalidAlgorithmParameterException e) {
            Log.e(TAG, "Lỗi khi tạo khóa bí mật trong Keystore", e);
            throw new RuntimeException("Không thể tạo khóa mã hóa. Xem Logcat với TAG '" + TAG + "'.", e);
        }
    }

    private SecretKey getSecretKey() throws KeyStoreException, NoSuchAlgorithmException, UnrecoverableEntryException {
        Log.d(TAG, "Đang lấy khóa bí mật từ Keystore với alias: " + KEY_ALIAS);
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            Log.e(TAG, "Không thể lấy khóa từ Keystore trên API level < 23");
            throw new KeyStoreException("Keystore không khả dụng cho phiên bản Android này.");
        }
        KeyStore.SecretKeyEntry secretKeyEntry = (KeyStore.SecretKeyEntry) keyStore.getEntry(KEY_ALIAS, null);
        if (secretKeyEntry != null) {
            Log.d(TAG, "Lấy khóa bí mật thành công.");
            return secretKeyEntry.getSecretKey();
        }
        Log.e(TAG, "Không tìm thấy khóa bí mật trong Keystore với alias: " + KEY_ALIAS + ". Thử tạo lại.");
        // Note: Nếu dữ liệu đã được mã hóa bằng khóa cũ, việc tạo khóa mới sẽ làm mất khả năng giải mã dữ liệu đó.
        try {
            generateSecretKey();
            secretKeyEntry = (KeyStore.SecretKeyEntry) keyStore.getEntry(KEY_ALIAS, null);
            if (secretKeyEntry != null) {
                Log.i(TAG, "Đã tạo lại và lấy được khóa bí mật.");
                return secretKeyEntry.getSecretKey();
            }
        } catch (Exception e) {
            Log.e(TAG, "Lỗi khi cố gắng tạo lại khóa.", e);
        }
        throw new UnrecoverableEntryException("Không tìm thấy khóa bí mật và không thể tạo lại: " + KEY_ALIAS);
    }

    public String encrypt(String plainText) {
        Log.d(TAG, "Bắt đầu quá trình mã hóa cho văn bản có độ dài: " + (plainText != null ? plainText.length() : "null"));
        if (plainText == null) {
            Log.w(TAG, "Văn bản gốc là null, không thể mã hóa.");
            return null;
        }
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            Log.e(TAG, "Mã hóa AES/GCM yêu cầu API level 23+.");
            return null;
        }
        try {
            SecretKey secretKey = getSecretKey();
            if (secretKey == null) {
                // Lỗi đã được log trong getSecretKey
                return null;
            }
            final Cipher cipher = Cipher.getInstance(AES_MODE_GCM);
            Log.d(TAG, "Cipher instance cho AES/GCM đã được tạo.");
            // KHÔNG cung cấp IV ở đây khi RandomizedEncryptionRequired là true.
            // Keystore sẽ tự tạo IV.
            cipher.init(Cipher.ENCRYPT_MODE, secretKey);
            Log.d(TAG, "Cipher đã được khởi tạo ở chế độ ENCRYPT (Keystore sẽ tạo IV).");
            byte[] cipherTextBytes = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));
            Log.d(TAG, "Dữ liệu đã được mã hóa thành công, độ dài ciphertext: " + cipherTextBytes.length);
            // Lấy IV đã được Cipher tạo ra
            byte[] iv = cipher.getIV();
            if (iv == null || iv.length != GCM_IV_LENGTH) {
                Log.e(TAG, "Lỗi lấy IV từ Cipher hoặc độ dài IV không đúng. IV là null? " + (iv == null) + (iv != null ? ", Độ dài IV: " + iv.length : ""));
                // Đối với một số nhà cung cấp Keystore, IV có thể cần được lấy trước doFinal nhưng thường là sau.
                // Nếu cipher.getIV() trả về null trước doFinal, logic này cần xem lại.
                // Tuy nhiên, tài liệu chuẩn nói rằng IV có sẵn sau init hoặc doFinal.
                return null; // Không thể tiếp tục nếu không có IV đúng
            }
            Log.d(TAG, "IV đã được lấy từ Cipher, độ dài: " + iv.length);
            // Ghép IV và ciphertext
            byte[] encryptedDataWithIv = new byte[iv.length + cipherTextBytes.length];
            System.arraycopy(iv, 0, encryptedDataWithIv, 0, iv.length);
            System.arraycopy(cipherTextBytes, 0, encryptedDataWithIv, iv.length, cipherTextBytes.length);
            Log.d(TAG, "IV và Ciphertext đã được ghép, tổng độ dài: " + encryptedDataWithIv.length);
            String base64Encoded = Base64.encodeToString(encryptedDataWithIv, Base64.NO_WRAP);
            Log.d(TAG, "Mã hóa hoàn tất, trả về chuỗi Base64.");
            return base64Encoded;

        } catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException |
                 IllegalBlockSizeException | BadPaddingException |
                 KeyStoreException | UnrecoverableEntryException e) {
            // InvalidAlgorithmParameterException không nên xảy ra nữa vì chúng ta không truyền IV
            Log.e(TAG, "Lỗi nghiêm trọng trong quá trình mã hóa dữ liệu!", e);
            return null;
        }  catch (Exception e) { // Bắt các ngoại lệ khác có thể xảy ra
            Log.e(TAG, "Lỗi không xác định trong quá trình mã hóa!", e);
            return null;
        }
    }

    public String decrypt(String encryptedDataWithIvBase64) {
        Log.d(TAG, "Bắt đầu quá trình giải mã...");
        if (encryptedDataWithIvBase64 == null) {
            Log.w(TAG, "Dữ liệu mã hóa (Base64) là null, không thể giải mã.");
            return null;
        }
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            Log.e(TAG, "Giải mã AES/GCM yêu cầu API level 23+.");
            return null;
        }
        try {
            SecretKey secretKey = getSecretKey();
            if (secretKey == null) {
                return null;
            }

            byte[] encryptedDataWithIv = Base64.decode(encryptedDataWithIvBase64, Base64.NO_WRAP);
            Log.d(TAG, "Dữ liệu Base64 đã được giải mã thành byte array, độ dài: " + encryptedDataWithIv.length);

            if (encryptedDataWithIv.length < GCM_IV_LENGTH) {
                Log.e(TAG, "Dữ liệu mã hóa không hợp lệ: quá ngắn để chứa IV. Độ dài: " + encryptedDataWithIv.length + ", IV yêu cầu: " + GCM_IV_LENGTH);
                return null;
            }
            byte[] iv = new byte[GCM_IV_LENGTH];
            System.arraycopy(encryptedDataWithIv, 0, iv, 0, iv.length);
            Log.d(TAG, "IV đã được tách, độ dài: " + iv.length);

            byte[] cipherTextBytes = new byte[encryptedDataWithIv.length - iv.length];
            System.arraycopy(encryptedDataWithIv, iv.length, cipherTextBytes, 0, cipherTextBytes.length);
            Log.d(TAG, "Ciphertext đã được tách, độ dài: " + cipherTextBytes.length);

            final Cipher cipher = Cipher.getInstance(AES_MODE_GCM);
            Log.d(TAG, "Cipher instance cho AES/GCM đã được tạo (giải mã).");
            GCMParameterSpec gcmParameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, gcmParameterSpec);
            Log.d(TAG, "Cipher đã được khởi tạo ở chế độ DECRYPT.");

            byte[] plainTextBytes = cipher.doFinal(cipherTextBytes);
            Log.d(TAG, "Dữ liệu đã được giải mã thành công.");
            return new String(plainTextBytes, StandardCharsets.UTF_8);

        } catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException |
                 InvalidAlgorithmParameterException | IllegalBlockSizeException | BadPaddingException |
                 KeyStoreException | UnrecoverableEntryException e) {
            Log.e(TAG, "Lỗi nghiêm trọng trong quá trình giải mã dữ liệu!", e);
            return null;
        } catch (IllegalArgumentException e) {
            Log.e(TAG, "Lỗi khi giải mã chuỗi Base64 (IllegalArgumentException). Dữ liệu vào có thể không phải Base64 hợp lệ.", e);
            return null;
        }  catch (Exception e) { // Bắt các ngoại lệ khác có thể xảy ra
            Log.e(TAG, "Lỗi không xác định trong quá trình giải mã!", e);
            return null;
        }
    }
}
