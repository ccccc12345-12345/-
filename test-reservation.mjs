const API = 'http://localhost:8080'

async function main() {
  const login = await (await fetch(`${API}/api/login`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ username: '18800000002', password: '123456', captchaKey: 'demo', captchaCode: 'demo' })
  })).json()
  const token = login.data.token
  console.log('token', token.slice(0, 40))

  const slotId = 39
  const res = await (await fetch(`${API}/api/reservation`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json', Authorization: `Bearer ${token}` },
    body: JSON.stringify({ slotId })
  })).json()
  console.log('reservation', JSON.stringify(res))
}

main().catch(e => console.error(e))
