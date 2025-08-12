import http from 'k6/http';
import { check, sleep } from 'k6';

export let options = {
  vus: 250,
  duration: '15s',
};

export default function () {

    let res = http.get('http://localhost:8080/db');

    check(res, {
        'status code is 200': (r) => r.status === 200
    });

}
