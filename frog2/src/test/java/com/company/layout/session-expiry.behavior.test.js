'use strict';

const assert = require('node:assert/strict');
const test = require('node:test');
const createSessionExpiry = require(
    '../../../../../main/webapp/resources/js/session-expiry.js');

function createHarness(contextPath = '/frog2') {
    const notifications = [];
    const redirects = [];
    const scheduled = [];
    const session = createSessionExpiry({
        contextPath,
        notify(message) {
            notifications.push(message);
        },
        redirect(url) {
            redirects.push(url);
        },
        schedule(callback, delay) {
            scheduled.push({callback, delay});
        }
    });
    return {notifications, redirects, scheduled, session};
}

test('successful and ordinary error responses remain untouched', () => {
    const harness = createHarness();
    const success = {status: 200};
    const badRequest = {status: 400};

    assert.equal(harness.session.requireActiveSession(success), success);
    assert.equal(harness.session.requireActiveSession(badRequest), badRequest);
    assert.deepEqual(harness.notifications, []);
    assert.deepEqual(harness.scheduled, []);
});

test('a 401 announces session expiry and redirects to the context login once', () => {
    const harness = createHarness('/frog2/');

    assert.throws(
        () => harness.session.requireActiveSession({status: 401}),
        (error) => harness.session.isSessionExpired(error));
    assert.throws(
        () => harness.session.requireActiveSession({status: 401}),
        (error) => harness.session.isSessionExpired(error));

    assert.deepEqual(harness.notifications, [
        '로그인 세션이 만료되었습니다. 다시 로그인해주세요.'
    ]);
    assert.equal(harness.scheduled.length, 1);
    assert.equal(harness.scheduled[0].delay, 900);

    harness.scheduled[0].callback();
    assert.deepEqual(harness.redirects, ['/frog2/login']);
});

test('an unsafe context path cannot become an open redirect target', () => {
    const harness = createHarness('//example.test/escape');

    assert.throws(
        () => harness.session.requireActiveSession({status: 401}),
        (error) => error.name === 'SessionExpiredError');
    harness.scheduled[0].callback();

    assert.deepEqual(harness.redirects, ['/login']);
});
