# NestJS example

`AnaMessagingModule.forRootAsync` reads credentials from `ConfigService`. A service then injects
`AnaMessagingClient` and exposes two endpoints:

```bash
ANAMESSAGING_CLIENT_ID=... ANAMESSAGING_CLIENT_SECRET=... npm start
curl -XPOST localhost:3000/notify/shipped -H 'content-type: application/json' \
  -d '{"phone":"919876543210","name":"Ravi","orderNo":"SO-1042"}'
```
