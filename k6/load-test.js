import http from 'k6/http';
import { check, sleep } from 'k6';

// Load test configuration
export const options = {
    scenarios: {
        // Scenario 1: Single deal imports
        single_deals: {
            executor: 'ramping-vus',
            exec: 'testSingleDeal',
            stages: [
                { duration: '30s', target: 10 },  // Ramp up to 10 users
                { duration: '1m', target: 10 },   // Stay at 10 users
                { duration: '10s', target: 0 },   // Ramp down
            ],
            gracefulStop: '10s',
        },
        // Scenario 2: Batch imports (concurrent with single)
        batch_imports: {
            executor: 'ramping-vus',
            exec: 'testBatchImport',
            startTime: '20s',  // Start 20s after single deals
            stages: [
                { duration: '20s', target: 3 },   // Ramp up to 3 users (batches are heavier)
                { duration: '1m', target: 3 },    // Stay at 3 users
                { duration: '10s', target: 0 },   // Ramp down
            ],
            gracefulStop: '10s',
        },
    },
    thresholds: {
        'http_req_duration{scenario:single_deals}': ['p(95)<1000'],     // Single deals < 1s
        'http_req_duration{scenario:batch_imports}': ['p(95)<3000'],    // Batch imports < 3s
        'http_req_failed': ['rate<0.1'],                                 // Error rate < 10%
        'checks': ['rate>0.95'],                                         // 95% of checks pass
    },
};

const BASE_URL = 'http://localhost:8080/api/deals';

// Generate unique deal ID
function generateDealId(prefix = 'DEAL') {
    const timestamp = Date.now();
    const random = Math.floor(Math.random() * 10000);
    const vu = __VU;
    const iter = __ITER;
    return `${prefix}-K6-${timestamp}-${vu}-${iter}-${random}`;
}

// Generate random currency pair
function getRandomCurrencyPair() {
    const currencies = [
        { from: 'USD', to: 'EUR' },
        { from: 'GBP', to: 'USD' },
        { from: 'EUR', to: 'JPY' },
        { from: 'USD', to: 'CHF' },
        { from: 'AUD', to: 'CAD' },
        { from: 'GBP', to: 'EUR' },
        { from: 'USD', to: 'JPY' },
    ];
    return currencies[Math.floor(Math.random() * currencies.length)];
}

// Generate random amount
function getRandomAmount() {
    return (Math.random() * 10000 + 100).toFixed(2);
}

// Test single deal creation
export function testSingleDeal() {
    const currencyPair = getRandomCurrencyPair();

    const payload = JSON.stringify({
        dealUniqueId: generateDealId('SINGLE'),
        fromCurrencyCode: currencyPair.from,
        toCurrencyCode: currencyPair.to,
        dealTimestamp: new Date().toISOString(),
        dealAmount: parseFloat(getRandomAmount())
    });

    const params = {
        headers: { 'Content-Type': 'application/json' },
        tags: { scenario: 'single_deals' },
    };

    const res = http.post(BASE_URL, payload, params);

    check(res, {
        'single deal: status is 201': (r) => r.status === 201,
        'single deal: has id': (r) => JSON.parse(r.body).id !== undefined,
        'single deal: response time < 1s': (r) => r.timings.duration < 1000,
    });

    sleep(1);
}

// Test batch import
export function testBatchImport() {
    const batchSize = 20;  // 20 deals per batch
    const deals = [];

    // Generate batch of deals
    for (let i = 0; i < batchSize; i++) {
        const currencyPair = getRandomCurrencyPair();
        deals.push({
            dealUniqueId: generateDealId(`BATCH-${i}`),
            fromCurrencyCode: currencyPair.from,
            toCurrencyCode: currencyPair.to,
            dealTimestamp: new Date().toISOString(),
            dealAmount: parseFloat(getRandomAmount())
        });
    }

    const payload = JSON.stringify({ deals: deals });

    const params = {
        headers: { 'Content-Type': 'application/json' },
        tags: { scenario: 'batch_imports' },
    };

    const res = http.post(`${BASE_URL}/batch`, payload, params);

    const passed = check(res, {
        'batch import: status is 201 or 207': (r) => r.status === 201 || r.status === 207,
        'batch import: has response': (r) => r.body.length > 0,
        'batch import: response time < 3s': (r) => r.timings.duration < 3000,
    });

    if (passed && (res.status === 201 || res.status === 207)) {
        const responseBody = JSON.parse(res.body);
        check(responseBody, {
            'batch import: has successful imports': (r) => r.successfulImports > 0,
            'batch import: total matches request': (r) => r.totalRequests === batchSize,
        });

        console.log(`Batch: ${responseBody.successfulImports}/${batchSize} successful`);
    }

    sleep(2);  // Longer sleep for batch operations
}

export function setup() {
    console.log('===========================================');
    console.log('Starting K6 Performance Test');
    console.log('===========================================');
    console.log(`Target URL: ${BASE_URL}`);
    console.log('Scenarios:');
    console.log('  - Single Deal Imports: 10 concurrent users');
    console.log('  - Batch Imports: 3 concurrent users (20 deals/batch)');
    console.log('===========================================');
}

export function teardown() {
    console.log('===========================================');
    console.log('Performance test completed!');
    console.log('===========================================');
}