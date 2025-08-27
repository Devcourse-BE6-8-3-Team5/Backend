package com.back.global.util;

import com.back.global.exception.ServiceException;

public class LevelSystem {

    public static int calculateLevel(int exp) {
        if (exp < 0) {
            throw new ServiceException(400, "경험치는 음수가 될 수 없습니다.");
        }

        if(exp<100) return 1;
        else if(exp<200) return 2;
        else return 3;
    }

    public static String getImageByLevel(int level) {
        switch (level) {
            case 1:
                return ""; // 레벨 1 이미지 URL ,src/main/resources/static/images에 저장
            case 2:
                return ""; // 레벨 2 이미지 URL
            case 3:
                return ""; // 레벨 3 이미지 URL
            default:
                throw new ServiceException(400, "유효하지 않은 레벨입니다.");
        }
    }
}
