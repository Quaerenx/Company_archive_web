'use strict';

const assert = require('node:assert/strict');
const test = require('node:test');
const {
    formatLicensePercentageHalfUp
} = require('../../../../../main/webapp/resources/js/pages/maintenance_form.js');

test('license percentage uses the same positive HALF_UP boundary as Java', () => {
    assert.equal(formatLicensePercentageHalfUp(0), '0.0');
    assert.equal(formatLicensePercentageHalfUp(55.6), '55.6');
    assert.equal(formatLicensePercentageHalfUp(89.949), '89.9');
    assert.equal(formatLicensePercentageHalfUp(89.95), '90.0');
    assert.equal(formatLicensePercentageHalfUp(105.049), '105.0');
    assert.equal(formatLicensePercentageHalfUp(105.05), '105.1');
});
