# Phase 1.5b — Component & Deployment diagram

> **Cần kiểm chứng**: Mermaid bản ổn định không có cú pháp `deploymentDiagram`
> chuẩn UML (chỉ có `C4Deploy` trong plugin mermaid-c4 ở một số bản, chưa
> chắc chắn có sẵn trong mọi phiên bản Mermaid mà dự án dùng). Dưới đây dùng
> `flowchart` với `subgraph` đại diện cho từng node vật lý — cách phổ biến
> và tương thích rộng nhất.

## Component diagram (logic, không phụ thuộc nơi deploy)

```mermaid
flowchart LR
    subgraph ClientSide["Trình duyệt"]
        SPA["React SPA (Vite build)"]
    end

    subgraph WebTier["Web/Reverse-proxy tier"]
        Nginx["Nginx\n(gzip, cache static, proxy /api)"]
    end

    subgraph AppTier["Application tier"]
        Spring["Spring Boot App\n(Controller → Service → Repository)"]
    end

    subgraph DataTier["Data tier"]
        PG[("PostgreSQL 16\n(unaccent, pg_trgm)")]
        Redis[("Redis 7\n(refresh token, rate limit,\nidempotency, phiên ca)")]
    end

    SPA -- "HTTPS" --> Nginx
    Nginx -- "serve static build" --> SPA
    Nginx -- "proxy /api/v1/*" --> Spring
    Spring -- "JDBC (HikariCP)" --> PG
    Spring -- "Lettuce" --> Redis
```

## Kịch bản triển khai 1 — VPS (Internet-facing)

```mermaid
flowchart TB
    subgraph Internet["Internet"]
        Customer["Trình duyệt nhân viên\n(từ xa hoặc tại cửa hàng)"]
    end

    subgraph VPS["VPS Ubuntu (public IP + domain)"]
        subgraph DockerHost["Docker Compose"]
            NginxC["container: nginx\n:443 (Let's Encrypt HTTPS), :80 → redirect 443"]
            ClientC["container: client\n(build tĩnh, serve qua nginx)"]
            ServerC["container: server\n(Spring Boot :8080)"]
            PgC[("container: postgres\nvolume: postgres-data")]
            RedisC[("container: redis")]
        end
    end

    Customer -- "HTTPS 443" --> NginxC
    NginxC -- "static files" --> ClientC
    NginxC -- "proxy /api" --> ServerC
    ServerC --> PgC
    ServerC --> RedisC
```

**Đặc điểm**: có domain + chứng chỉ HTTPS (Let's Encrypt, certbot); truy cập
được từ bất kỳ đâu có Internet; cần cấu hình firewall (UFW) chỉ mở 80/443/22.

## Kịch bản triển khai 2 — Máy LAN nội bộ (không cần domain)

```mermaid
flowchart TB
    subgraph LAN["Mạng LAN nội bộ cửa hàng"]
        POS1["Máy POS 1\n(trình duyệt, IP nội bộ)"]
        POS2["Máy POS 2"]
        subgraph ServerMachine["Máy chủ nội bộ (IP tĩnh, vd 192.168.1.10)"]
            subgraph DockerHost2["Docker Compose"]
                NginxL["container: nginx :80"]
                ClientL["container: client"]
                ServerL["container: server :8080"]
                PgL[("container: postgres")]
                RedisL[("container: redis")]
            end
        end
    end

    POS1 -- "HTTP 80 (nội bộ, không cần HTTPS)" --> NginxL
    POS2 -- "HTTP 80" --> NginxL
    NginxL --> ClientL
    NginxL -- "proxy /api" --> ServerL
    ServerL --> PgL
    ServerL --> RedisL
```

**Đặc điểm**: truy cập qua IP tĩnh nội bộ, không cần domain/HTTPS (mạng
riêng, không public ra Internet); firewall Windows/UFW chỉ cho phép các máy
trong dải IP LAN truy cập cổng 80; backup `pg_dump` định kỳ vẫn áp dụng như
kịch bản VPS.

## So sánh nhanh 2 kịch bản

| Tiêu chí | VPS | LAN nội bộ |
|---|---|---|
| HTTPS | Bắt buộc (Let's Encrypt) | Không cần (mạng riêng) |
| Domain | Cần | Không cần, dùng IP tĩnh |
| Truy cập từ xa | Có | Không (chỉ trong LAN, trừ khi VPN) |
| Chi phí vận hành | Thuê VPS hàng tháng | Chỉ 1 máy chủ tại chỗ, không phí thuê |
| Firewall | UFW mở 80/443/22 | UFW/Windows Firewall giới hạn theo dải IP LAN |
| Phù hợp | Cửa hàng muốn quản lý từ xa, nhiều chi nhánh | Cửa hàng đơn lẻ, ngân sách hạn chế |

Cả 2 kịch bản dùng chung 1 bộ `docker-compose.yml` + `.env` khác nhau (biến
môi trường `CORS_ALLOWED_ORIGINS`, có/không có Nginx cấu hình SSL) — chi
tiết Dockerfile/compose sinh ở Phase 12.
