from jwt import InvalidTokenError
from sqlalchemy.exc import IntegrityError

from app.core.security import AuthSecurity
from app.models.user import User
from app.repositories.users import UserRepository
from app.schemas.auth import LoginRequest, RegisterRequest, TokenResponse


class AccountConflictError(Exception):
    pass


class InvalidCredentialsError(Exception):
    pass


class AuthService:
    def __init__(self, users: UserRepository, security: AuthSecurity) -> None:
        self.users = users
        self.security = security

    def register(self, request: RegisterRequest) -> User:
        password_hash = self.security.hash_password(request.password.get_secret_value())
        try:
            return self.users.create(
                username=request.username, email=request.email, password_hash=password_hash
            )
        except IntegrityError as error:
            constraint = getattr(getattr(error.orig, "diag", None), "constraint_name", None)
            if constraint == "uq_users_email":
                raise AccountConflictError("该邮箱已注册") from error
            if constraint == "uq_users_username_key":
                raise AccountConflictError("用户名已被使用") from error
            raise

    def login(self, request: LoginRequest) -> TokenResponse:
        user = self.users.get_by_email(request.email)
        verified = self.security.verify_password(
            request.password.get_secret_value(), user.password_hash if user else None
        )
        if not verified or user is None or not user.is_active:
            raise InvalidCredentialsError("邮箱或密码不正确")
        return TokenResponse(
            access_token=self.security.create_access_token(user.id),
            expires_in=self.security.settings.access_token_expire_minutes * 60,
        )

    def current_user(self, token: str) -> User:
        try:
            user_id = self.security.decode_access_token(token)
        except InvalidTokenError as error:
            raise InvalidCredentialsError("登录状态已失效，请重新登录") from error
        user = self.users.get_by_id(user_id)
        if user is None or not user.is_active:
            raise InvalidCredentialsError("登录状态已失效，请重新登录")
        return user
