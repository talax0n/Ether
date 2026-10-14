// Polls Mailpit for the newest sign-in email sent to EMAIL and exposes its code as output.otp.
const base = MAILPIT_URL || 'http://127.0.0.1:54324'
const query = encodeURIComponent('to:"' + EMAIL + '"')
let code = null
for (let i = 0; i < 200 && !code; i++) {
  const res = http.get(base + '/api/v1/search?limit=1&query=' + query)
  const messages = json(res.body).messages || []
  const match = messages.length ? /\b(\d{6})\b/.exec(messages[0].Snippet) : null
  if (match) code = match[1]
}
if (!code) throw new Error('No OTP email for ' + EMAIL)
output.otp = code
