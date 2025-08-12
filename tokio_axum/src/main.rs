#[global_allocator]
static GLOBAL: mimalloc::MiMalloc = mimalloc::MiMalloc;

use axum::{
    Json, Router,
    extract::FromRequestParts,
    http::{StatusCode, request::Parts},
    response::IntoResponse,
    routing::get,
};

use deadpool_postgres::{Client, GenericClient, Manager, ManagerConfig, RecyclingMethod};
use tokio_postgres::NoTls;

#[derive(serde::Serialize)]
struct User {
    name: String,
}

pub struct DatabaseClient(pub Client);

impl FromRequestParts<deadpool_postgres::Pool> for DatabaseClient {
    type Rejection = (StatusCode, String);

    async fn from_request_parts(
        _parts: &mut Parts,
        pool: &deadpool_postgres::Pool,
    ) -> Result<Self, Self::Rejection> {
        let conn = pool.get().await.map_err(internal_error)?;

        Ok(Self(conn))
    }
}

pub async fn create_pool(database_url: String, max_pool_size: u32) -> deadpool_postgres::Pool {
    let pg_config: tokio_postgres::Config = database_url.parse().expect("invalid database url");

    let mgr_config = ManagerConfig {
        recycling_method: RecyclingMethod::Fast,
    };
    let mgr = Manager::from_config(pg_config, NoTls, mgr_config);
    let pool: deadpool_postgres::Pool = deadpool_postgres::Pool::builder(mgr)
        .max_size(max_pool_size as usize)
        .build()
        .unwrap();

    pool
}

pub fn internal_error<E>(err: E) -> (StatusCode, String)
where
    E: std::error::Error,
{
    (StatusCode::INTERNAL_SERVER_ERROR, err.to_string())
}

async fn db(DatabaseClient(client): DatabaseClient) -> impl IntoResponse {
    let select = &client.prepare_cached("select * from users;").await.unwrap();
    let rows = client
        .query(select, &[])
        .await
        .expect("could not fetch world");

    let data: Vec<User> = rows
        .into_iter()
        .map(|row| User {
            name: row.get("name"),
        })
        .collect();

    (StatusCode::OK, Json(data))
}

#[tokio::main]
async fn main() {
    let pg_pool = create_pool("postgres://postgres:fuji123@localhost/tes".to_string(), 16).await;

    let app = Router::new()
        .route("/", get(json))
        .route("/db", get(db))
        .with_state(pg_pool);

    let listener = tokio::net::TcpListener::bind("localhost:8080")
        .await
        .unwrap();
    axum::serve(listener, app).await.unwrap();
}

async fn json() -> (StatusCode, Json<&'static str>) {
    (StatusCode::OK, Json("Hellooo"))
}
