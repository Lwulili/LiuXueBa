from fastapi import APIRouter
from sqlalchemy import text

from app.api.dependencies import SessionDependency

router = APIRouter(tags=["服务状态"])


@router.get("/health")
def health(session: SessionDependency) -> dict[str, str]:
    session.execute(text("SELECT 1"))
    return {"status": "ok", "database": "ok"}
