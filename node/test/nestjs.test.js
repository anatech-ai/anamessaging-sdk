require('reflect-metadata');
const test = require('node:test');
const assert = require('node:assert');
const { Test } = require('@nestjs/testing');
const { AnaMessagingModule, ANA_MESSAGING_CLIENT, AnaMessagingClient } = require('@anamessaging/sdk/nestjs');

test('forRoot and forRootAsync provide the client', async () => {
  const a = await Test.createTestingModule({ imports: [AnaMessagingModule.forRoot({ clientId: 'c', clientSecret: 's' })] }).compile();
  assert.ok(a.get(ANA_MESSAGING_CLIENT) instanceof AnaMessagingClient);
  assert.equal(a.get(AnaMessagingClient), a.get(ANA_MESSAGING_CLIENT));
  const b = await Test.createTestingModule({
    imports: [AnaMessagingModule.forRootAsync({ useFactory: async () => ({ clientId: 'c', clientSecret: 's', baseUrl: 'http://x' }) })],
  }).compile();
  assert.ok(b.get(AnaMessagingClient).whatsapp);
});
