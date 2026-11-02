import 'reflect-metadata';
import { Body, Controller, Injectable, Module, Post } from '@nestjs/common';
import { ConfigModule, ConfigService } from '@nestjs/config';
import { NestFactory } from '@nestjs/core';
import { AnaMessagingClient, SendSmsRequest } from '@anamessaging/sdk';
import { AnaMessagingModule } from '@anamessaging/sdk/nestjs';

@Injectable()
class NotificationService {
  // Injected by class; `@InjectAnaMessaging()` works too.
  constructor(private readonly ana: AnaMessagingClient) {}

  orderShipped(phone: string, name: string, orderNo: string) {
    return this.ana.whatsapp.sendTemplate({
      to: phone,
      templateName: 'order_shipped',
      components: [{ type: 'body', parameters: [{ type: 'text', text: name }, { type: 'text', text: orderNo }] }],
      campaignId: orderNo,
    });
  }

  otp(request: SendSmsRequest) {
    return this.ana.sms.send(request);
  }
}

@Controller('notify')
class NotifyController {
  constructor(private readonly notifications: NotificationService) {}

  @Post('shipped')
  shipped(@Body() b: { phone: string; name: string; orderNo: string }) {
    return this.notifications.orderShipped(b.phone, b.name, b.orderNo);
  }

  @Post('sms')
  sms(@Body() b: SendSmsRequest) {
    return this.notifications.otp(b);
  }
}

@Module({
  imports: [
    ConfigModule.forRoot(),
    AnaMessagingModule.forRootAsync({
      inject: [ConfigService],
      useFactory: (config: ConfigService) => ({
        clientId: config.getOrThrow('ANAMESSAGING_CLIENT_ID'),
        clientSecret: config.getOrThrow('ANAMESSAGING_CLIENT_SECRET'),
        baseUrl: config.get('ANAMESSAGING_BASE_URL'),
      }),
    }),
  ],
  controllers: [NotifyController],
  providers: [NotificationService],
})
class AppModule {}

NestFactory.create(AppModule).then((app) => app.listen(3000));
