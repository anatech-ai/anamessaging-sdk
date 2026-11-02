import test from 'node:test';
import assert from 'node:assert';
import ana, { AnaMessagingClient, SubscriptionInactiveError } from '@anamessaging/sdk';

test('ESM named + default imports work', () => {
  assert.equal(typeof AnaMessagingClient, 'function');
  assert.equal(typeof SubscriptionInactiveError, 'function');
  assert.equal(ana.AnaMessagingClient, AnaMessagingClient);
});
