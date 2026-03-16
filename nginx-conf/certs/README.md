# 로컬 Nginx TLS 인증서 안내

로컬 Nginx HTTPS에서 사용할 TLS 인증서를 이 폴더에 두세요.

- `fullchain.pem`
- `privkey.pem`

로컬 테스트용 self-signed 인증서 생성 예시:

```bash
docker run --rm -v "$(pwd)/nginx-conf/certs:/out" alpine:3.20 sh -c \
  "apk add --no-cache openssl >/dev/null && \
   openssl req -x509 -nodes -newkey rsa:2048 \
   -keyout /out/privkey.pem \
   -out /out/fullchain.pem \
   -days 365 \
   -subj '/CN=api.dukku.earlydreamer.dev' \
   -addext 'subjectAltName=DNS:api.dukku.earlydreamer.dev,DNS:localhost'"
```

참고:
- `default.conf`에서 인증서 경로를 `/etc/nginx/certs/fullchain.pem`, `/etc/nginx/certs/privkey.pem`로 참조합니다.
- 이 경로는 `docker-compose.local.nginx.yml`의 볼륨 마운트(`./nginx-conf/certs:/etc/nginx/certs:ro`)로 연결됩니다.
