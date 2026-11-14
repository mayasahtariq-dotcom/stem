"""API errors as short codes. The app maps each code to a bilingual message."""

from enum import StrEnum

from fastapi import HTTPException, status


class ErrorCode(StrEnum):
    UNAUTHORIZED = "UNAUTHORIZED"
    PROFILE_INCOMPLETE = "PROFILE_INCOMPLETE"
    NOT_A_TEACHER = "NOT_A_TEACHER"
    NOT_A_STUDENT = "NOT_A_STUDENT"
    NOT_FOUND = "NOT_FOUND"
    KEY_NOT_FOUND = "KEY_NOT_FOUND"
    KEY_EXPIRED = "KEY_EXPIRED"
    ALREADY_MEMBER = "ALREADY_MEMBER"
    LANGUAGE_NOT_ALLOWED = "LANGUAGE_NOT_ALLOWED"
    TOO_MANY_ATTEMPTS = "TOO_MANY_ATTEMPTS"
    KEY_GENERATION_FAILED = "KEY_GENERATION_FAILED"


_STATUS = {
    ErrorCode.UNAUTHORIZED: status.HTTP_401_UNAUTHORIZED,
    ErrorCode.PROFILE_INCOMPLETE: status.HTTP_409_CONFLICT,
    ErrorCode.NOT_A_TEACHER: status.HTTP_403_FORBIDDEN,
    ErrorCode.NOT_A_STUDENT: status.HTTP_403_FORBIDDEN,
    ErrorCode.NOT_FOUND: status.HTTP_404_NOT_FOUND,
    ErrorCode.KEY_NOT_FOUND: status.HTTP_404_NOT_FOUND,
    ErrorCode.KEY_EXPIRED: status.HTTP_410_GONE,
    ErrorCode.ALREADY_MEMBER: status.HTTP_409_CONFLICT,
    ErrorCode.LANGUAGE_NOT_ALLOWED: status.HTTP_403_FORBIDDEN,
    ErrorCode.TOO_MANY_ATTEMPTS: status.HTTP_429_TOO_MANY_REQUESTS,
    ErrorCode.KEY_GENERATION_FAILED: status.HTTP_503_SERVICE_UNAVAILABLE,
}


def api_error(code: ErrorCode) -> HTTPException:
    return HTTPException(status_code=_STATUS[code], detail={"code": code.value})
