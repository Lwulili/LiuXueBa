from fastapi import APIRouter, Depends, Response, status

from app.api.dependencies import AuthServiceDependency, CurrentUserDependency
from app.schemas.auth import LoginRequest, RegisterRequest, TokenResponse, UserResponse


def prevent_caching(response: Response) -> None:
    response.headers["Cache-Control"] = "no-store"


router = APIRouter(prefix="/auth", tags=["账号"], dependencies=[Depends(prevent_caching)])


@router.post("/register", response_model=UserResponse, status_code=status.HTTP_201_CREATED)
def register(request: RegisterRequest, service: AuthServiceDependency):
    return service.register(request)


@router.post("/login", response_model=TokenResponse)
def login(request: LoginRequest, service: AuthServiceDependency) -> TokenResponse:
    return service.login(request)


@router.get("/me", response_model=UserResponse)
def current_user(user: CurrentUserDependency):
    return user
