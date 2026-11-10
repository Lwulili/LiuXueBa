from datetime import UTC, datetime, timedelta
from uuid import UUID

import jwt
from jwt import InvalidTokenError
from pwdlib import PasswordHash
from pwdlib.exceptions import UnknownHashError

from app.core.config import Settings


class AuthSecurity:
    def __init__(self, settings: Settings) -> None:
        self.settings = settings
        self._password_hash = PasswordHash.recommended()
        # Unknown accounts still perform a password verification.
        self._dummy_hash = self._password_hash.hash("unused-dummy-password")

    def hash_password(self, password: str) -> str:
        return self._password_hash.hash(password)

    def verify_password(self, password: str, password_hash: str | None) -> bool:
        try:
            verified = self._password_hash.verify(password, password_hash or self._dummy_hash)
        except (UnknownHashError, ValueError):
            return False
        return password_hash is not None and verified

    def create_access_token(self, user_id: UUID) -> str:
        now = datetime.now(UTC)
        return jwt.encode(
            {
                "sub": str(user_id),
                "iat": now,
                "nbf": now,
                "exp": now + timedelta(minutes=self.settings.access_token_expire_minutes),
                "iss": self.settings.jwt_issuer,
                "aud": self.settings.jwt_audience,
            },
            self.settings.jwt_secret_key.get_secret_value(),
            algorithm="HS256",
        )

    def decode_access_token(self, token: str) -> UUID:
        claims = jwt.decode(
            token,
            self.settings.jwt_secret_key.get_secret_value(),
            algorithms=["HS256"],
            issuer=self.settings.jwt_issuer,
            audience=self.settings.jwt_audience,
            options={"require": ["sub", "iat", "nbf", "exp", "iss", "aud"]},
        )
        try:
            return UUID(claims["sub"])
        except (ValueError, TypeError, AttributeError) as error:
            raise InvalidTokenError("Invalid token subject") from error
