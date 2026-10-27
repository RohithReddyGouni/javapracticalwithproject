import http from 'k6/http';
import { check, sleep } from 'k6';

const baseUrl = __ENV.BASE_URL;
const hotelId = __ENV.HOTEL_ID;
const token = __ENV.AUTH_TOKEN;

if (!baseUrl || !hotelId || !token) {
  throw new Error(
    'BASE_URL, HOTEL_ID, and AUTH_TOKEN are required'
  );
}

export const options = {
  vus: 25,
  duration: '60s',
  gracefulStop: '5s',

  thresholds: {
    http_req_failed: ['rate<0.01'],
    http_req_duration: ['p(95)<500'],
    checks: ['rate>0.99'],
  },
};

function getHotel() {
  return http.get(
    `${baseUrl}/admin/hotels/${hotelId}`,
    {
      headers: {
        Authorization: `Bearer ${token}`,
      },
      timeout: '5s',
      tags: {
        name: 'GET /admin/hotels/{hotelId}',
      },
    }
  );
}

export function setup() {
  const response = getHotel();

  if (response.status !== 200) {
    throw new Error(
      `Initial request returned ${response.status}. ` +
      'Check the token, hotel ID, and ownership.'
    );
  }
}

export default function () {
  const response = getHotel();

  check(response, {
    'hotel lookup returns HTTP 200':
      (result) => result.status === 200,
  });

  sleep(1);
}