package com.project.imgapi.perf;

import java.security.SecureRandom;

public class BytesFactory {
    private static final SecureRandom rnd = new SecureRandom();
    private static volatile byte[] cached;

    public static byte[] randomBytes(int size, boolean duplicateStick) {
        if (duplicateStick) {
            if (cached == null || cached.length != size) {
                cached = create(size);
            }
            return cached.clone();
        }
        return create(size);
    }

    private static byte[] create(int size) {
        byte[] buf = new byte[size];
        rnd.nextBytes(buf);
        // JPEG 흉내 대신 실제 유효 이미지가 필요하면 테스트용 validJpegBytes로 교체
        return buf;
    }
}
