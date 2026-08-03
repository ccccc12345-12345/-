const API = 'http://localhost:8080'

async function main() {
  const login = await (await fetch(`${API}/api/login`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ username: '18800000002', password: '123456', captchaKey: 'demo', captchaCode: 'demo' })
  })).json()
  const token = login.data.token
  const res = await (await fetch(`${API}/api/reservation/my`, { headers: { Authorization: `Bearer ${token}` } })).json()
  console.log(JSON.stringify(res, null, 2))
}
main().catch(e => console.error(e))
