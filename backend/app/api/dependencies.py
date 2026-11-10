from collections.abc import Iterator
from typing import Annotated

from fastapi import Depends, Request, Security
from fastapi.security import HTTPAuthorizationCredentials, HTTPBearer
from sqlalchemy.orm import Session

from app.models.user import User
from app.repositories.users import UserRepository
from app.services.auth import AuthService, InvalidCredentialsError

_bearer = HTTPBearer(auto_error=False, scheme_name="AccessToken")


def get_session(request: Request) -> Iterator[Session]:
    with request.app.state.database.sessions() as session:
        yield session


SessionDependency = Annotated[Session, Depends(get_session)]


def get_auth_service(request: Request, session: SessionDependency) -> AuthService:
    return AuthService(UserRepository(session), request.app.state.security)


AuthServiceDependency = Annotated[AuthService, Depends(get_auth_service)]


def get_current_user(
    credentials: Annotated[HTTPAuthorizationCredentials | None, Security(_bearer)],
    service: AuthServiceDependency,
) -> User:
    if credentials is None:
        raise InvalidCredentialsError("请先登录")
    return service.current_user(credentials.credentials)


CurrentUserDependency = Annotated[User, Depends(get_current_user)]
