FROM groonga/pgroonga:latest-debian-18

# -----------------------------
# pgvector 설치 (PGDG 리포에서)
# -----------------------------
RUN apt-get update \
    && apt-get install -y postgresql-18-pgvector \
    && apt-get clean && rm -rf /var/lib/apt/lists/*
