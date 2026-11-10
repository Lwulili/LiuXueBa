import re
from datetime import datetime
from typing import Literal
from uuid import UUID

from pydantic import BaseModel, ConfigDict, SecretStr, field_validator

_EMAIL_PATTERN = re.compile(
    r"[A-Za-z0-9!#$%&'*+/=?^_`{|}~-]+(?:\.[A-Za-z0-9!#$%&'*+/=?^_`{|}~-]+)*@"
    r"[A-Za-z0-9](?:[A-Za-z0-9-]{0,61}[A-Za-z0-9])?"
    r"(?:\.[A-Za-z0-9](?:[A-Za-z0-9-]{0,61}[A-Za-z0-9])?)+"
)


def _android_length(value: str) -> int:
    """Kotlin String.length counts UTF-16 units; encoding also rejects lone surrogates."""
    try:
        return len(value.encode("utf-16-le")) // 2
    except UnicodeEncodeError as error:
        raise ValueError("输入包含不支持的字符") from error


class LoginRequest(BaseModel):
    model_config = ConfigDict(extra="forbid", strict=True)

    email: str
    password: SecretStr

    @field_validator("email")
    @classmethod
    def validate_email(cls, value: str) -> str:
        email = value.strip().lower()
        if (
            len(email) > 254
            or len(email.split("@", 1)[0]) > 64
            or not _EMAIL_PATTERN.fullmatch(email)
        ):
            raise ValueError("请输入有效的邮箱地址")
        return email

    @field_validator("password")
    @classmethod
    def validate_password(cls, value: SecretStr) -> SecretStr:
        password = value.get_secret_value()
        if not password.strip() or not 8 <= _android_length(password) <= 64:
            raise ValueError("密码需要 8—64 个字符")
        return value


class RegisterRequest(LoginRequest):
    username: str

    @field_validator("username")
    @classmethod
    def validate_username(cls, value: str) -> str:
        username = value.strip()
        if "\x00" in username:
            raise ValueError("用户名包含不支持的字符")
        if not 2 <= _android_length(username) <= 20:
            raise ValueError("用户名需要 2—20 个字符")
        if any(character.isspace() for character in username):
            raise ValueError("用户名不能包含空格")
        return username


class UserResponse(BaseModel):
    model_config = ConfigDict(from_attributes=True)

    id: UUID
    username: str
    email: str
    nickname: str
    role: Literal["user", "admin"]
    created_at: datetime


class TokenResponse(BaseModel):
    access_token: str
    token_type: Literal["bearer"] = "bearer"
    expires_in: int
