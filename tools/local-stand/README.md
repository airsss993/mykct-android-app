# Локальный стенд

Боевого API нет (см. HANDOFF), портал колледжа виден только из его сети. Стенд поднимает
**настоящий** `college-app-core` поверх заглушки портала — так клиент проверяется против
реальных ответов бэкенда, а не против выдуманного JSON.

```bash
# 1. собрать бэкенд (репозиторий рядом: ~/Desktop/2-Проекты/MeAndProjects/Repositories)
go build -o /tmp/stand/core ~/.../college-app-core/cmd/app

# 2. разложить конфиг рядом с бинарником: viper читает ./configs/main.yml от cwd
cp -r tools/local-stand/{configs,stub.py,mode} /tmp/stand/
cp tools/local-stand/env.example /tmp/stand/.env

# 3. запустить: заглушка портала на :9911, core-api на :8500
cd /tmp/stand && python3 stub.py & ./core &

# 4. собрать debug с адресами стенда (эмулятор видит хост как 10.0.2.2)
#    в local.properties: API_BASE_URL=http://10.0.2.2:8500
#                        AUTH_BASE_URL=http://10.0.2.2:9911
```

Cleartext-http разрешён только для debug — `app/src/debug/res/xml/network_security_config.xml`.

`mode` переключает, что отдаёт портал, читается на каждом запросе:
`normal` — данные, `empty` — `[]`, `null` — `null` (бэкенд в этом случае отдаёт голый
`null` вместо `[]`, клиент это переживает).

Заглушка отвечает и за auth-сервис (`/auth/api/v1/app/*`): логин любой, пароль от 6 знаков.
