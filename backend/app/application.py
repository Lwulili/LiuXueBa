import logging
from collections.abc import AsyncIterator
from contextlib import asynccontextmanager

from fastapi import FastAPI, Request
from fastapi.exceptions import RequestValidationError
from fastapi.responses import JSONResponse
from sqlalchemy.exc import SQLAlchemyError

from app.api import auth, health
from app.core.config import Settings, get_settings
from app.core.security import AuthSecurity
from app.db.session import Database
from app.services.auth import AccountConflictError, InvalidCredentialsError

logger = logging.getLogger(__name__)


def create_app(settings: Settings | None = None) -> FastAPI:
    settings = settings or get_settings()
    database = Database(settings.database_url.get_secret_value())

    @asynccontextmanager
    async def lifespan(app: FastAPI) -> AsyncIterator[None]:
        try:
            yield
        finally:
            database.dispose()

    app = FastAPI(title="留学吧 API", version="0.1.0", lifespan=lifespan)
    app.state.settings = settings
    app.state.database = database
    app.state.security = AuthSecurity(settings)
    app.include_router(health.router)
    app.include_router(auth.router)

    @app.exception_handler(AccountConflictError)
    async def account_conflict(request: Request, error: AccountConflictError) -> JSONResponse:
        return JSONResponse(
            status_code=409, content={"detail": str(error)}, headers={"Cache-Control": "no-store"}
        )

    @app.exception_handler(InvalidCredentialsError)
    async def invalid_credentials(request: Request, error: InvalidCredentialsError) -> JSONResponse:
        return JSONResponse(
            status_code=401,
            content={"detail": str(error)},
            headers={"WWW-Authenticate": "Bearer", "Cache-Control": "no-store"},
        )

    @app.exception_handler(RequestValidationError)
    async def invalid_request(request: Request, error: RequestValidationError) -> JSONResponse:
        # FastAPI's default error includes the submitted input, which may contain a password.
        details = [
            {"loc": item["loc"], "msg": item["msg"], "type": item["type"]}
            for item in error.errors()
        ]
        return JSONResponse(
            status_code=422, content={"detail": details}, headers={"Cache-Control": "no-store"}
        )

    @app.exception_handler(SQLAlchemyError)
    async def database_failure(request: Request, error: SQLAlchemyError) -> JSONResponse:
        logger.error("Database request failed (%s)", type(error).__name__)
        return JSONResponse(
            status_code=503,
            content={"detail": "服务暂时不可用，请稍后重试"},
            headers={"Cache-Control": "no-store"},
        )

    return app
