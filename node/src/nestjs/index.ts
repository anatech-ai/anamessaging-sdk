import { DynamicModule, Global, Inject, Module, Provider } from '@nestjs/common';
import { AnaMessagingClient } from '../client';
import { AnaMessagingConfig } from '../config';

/** Injection token for the client (the class itself also works as a token). */
export const ANA_MESSAGING_CLIENT = 'ANA_MESSAGING_CLIENT';

export interface AnaMessagingModuleAsyncOptions {
  imports?: any[];
  inject?: any[];
  useFactory: (...args: any[]) => AnaMessagingConfig | Promise<AnaMessagingConfig>;
  /** Register globally (default true). */
  isGlobal?: boolean;
}

/** `@InjectAnaMessaging() private readonly ana: AnaMessagingClient` */
export const InjectAnaMessaging = (): ParameterDecorator & PropertyDecorator => Inject(ANA_MESSAGING_CLIENT);

/**
 * NestJS integration.
 *
 * ```ts
 * @Module({
 *   imports: [
 *     AnaMessagingModule.forRootAsync({
 *       inject: [ConfigService],
 *       useFactory: (c: ConfigService) => ({
 *         clientId: c.get('ANAMESSAGING_CLIENT_ID'),
 *         clientSecret: c.get('ANAMESSAGING_CLIENT_SECRET'),
 *       }),
 *     }),
 *   ],
 * })
 * export class AppModule {}
 * ```
 */
@Global()
@Module({})
export class AnaMessagingModule {
  static forRoot(config: AnaMessagingConfig = {}, isGlobal = true): DynamicModule {
    return this.build([{ provide: ANA_MESSAGING_CLIENT, useFactory: () => new AnaMessagingClient(config) }], [], isGlobal);
  }

  static forRootAsync(options: AnaMessagingModuleAsyncOptions): DynamicModule {
    const provider: Provider = {
      provide: ANA_MESSAGING_CLIENT,
      inject: options.inject ?? [],
      useFactory: async (...args: any[]) => new AnaMessagingClient(await options.useFactory(...args)),
    };
    return this.build([provider], options.imports ?? [], options.isGlobal ?? true);
  }

  private static build(providers: Provider[], imports: any[], isGlobal: boolean): DynamicModule {
    const alias: Provider = { provide: AnaMessagingClient, useExisting: ANA_MESSAGING_CLIENT };
    return {
      module: AnaMessagingModule,
      global: isGlobal,
      imports,
      providers: [...providers, alias],
      exports: [ANA_MESSAGING_CLIENT, AnaMessagingClient],
    };
  }
}

export { AnaMessagingClient };
