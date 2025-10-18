(function () {
  const root = document.querySelector('[data-auth-root]')
  if (!root) {
    return
  }

  const card = root.querySelector('.auth-card')
  const loginSection = root.querySelector('[data-view="signin"]')
  const registerSection = root.querySelector('[data-view="register"]')
  const registerForm = root.querySelector('#register-form')

  const registerTitle = registerSection?.querySelector('.auth-title')
  const registerTitleDefault = registerTitle?.dataset?.titleDefault || registerTitle?.textContent?.trim() || ''
  const registerTitleSuccess = registerTitle?.dataset?.titleSuccess || registerTitleDefault

  const successContainer = registerSection?.querySelector('[data-register-success]')
  const successMessage = successContainer?.querySelector('[data-success-message]')
  const successFeedback = successContainer?.querySelector('[data-success-feedback]')
  let successResendButton = successContainer?.querySelector('[data-action="resend-verification"]')
  let resendCooldownTimer = null
  let resendCooldownExpiresAt = 0
  let resendCooldownDetail = ''
  const successHomeLink = successContainer?.querySelector('[data-action="go-home"]')
  const registerNote = registerSection?.querySelector('.auth-note')
  const otpModal = registerSection?.querySelector('[data-otp-modal]')
  const otpInput = registerSection?.querySelector('[data-otp-input]')
  const otpFeedback = registerSection?.querySelector('[data-otp-feedback]')

  const passwordToggleButtons = Array.from(root.querySelectorAll('[data-toggle-password]'))

  if (registerSection && !registerSection.dataset.state) {
    registerSection.dataset.state = 'form'
  }

  function setRegisterState(state) {
    if (registerSection) {
      registerSection.dataset.state = state
    }
    if (registerTitle) {
      const nextTitle = state === 'success' ? registerTitleSuccess : registerTitleDefault
      if (nextTitle) {
        registerTitle.textContent = nextTitle
      }
    }
    if (registerNote) {
      if (state === 'success') {
        registerNote.hidden = true
        registerNote.setAttribute('aria-hidden', 'true')
      } else {
        registerNote.hidden = false
        registerNote.removeAttribute('aria-hidden')
      }
    }
  }

  function measureViewHeight(view) {
    if (!card || !view) {
      return 0
    }

    const wrapper = document.createElement('div')
    wrapper.className = 'auth-card'
    wrapper.style.position = 'absolute'
    wrapper.style.visibility = 'hidden'
    wrapper.style.pointerEvents = 'none'
    wrapper.style.left = '-9999px'
    wrapper.style.width = `${card.offsetWidth}px`

    const clone = view.cloneNode(true)
    clone.removeAttribute('hidden')
    clone.removeAttribute('aria-hidden')
    clone.style.display = 'grid'
    clone.querySelectorAll('[id]').forEach((element) => element.removeAttribute('id'))

    wrapper.appendChild(clone)
    document.body.appendChild(wrapper)

    const height = wrapper.getBoundingClientRect().height
    document.body.removeChild(wrapper)
    return height
  }

  function syncCardHeight() {
    if (!card) {
      return
    }
    const signinHeight = measureViewHeight(loginSection)
    const registerHeight = measureViewHeight(registerSection)
    const targetHeight = Math.max(signinHeight, registerHeight)
    if (targetHeight > 0) {
      card.style.minHeight = `${targetHeight}px`
    }
  }

  let resizeHandle
  function handleResize() {
    if (resizeHandle) {
      window.clearTimeout(resizeHandle)
    }
    resizeHandle = window.setTimeout(syncCardHeight, 150)
  }

  window.addEventListener('resize', handleResize)

  function updateDocumentTitle(mode) {
    if (!card) {
      return
    }
    const title = mode === 'register' ? card.dataset.registerTitle : card.dataset.signinTitle
    if (title) {
      document.title = title
    }
  }

  function setMode(mode) {
    if (!card) {
      return
    }
    card.setAttribute('data-mode', mode)
    showRegisterForm()

    if (registerSection) {
      const isRegister = mode === 'register'
      registerSection.hidden = !isRegister
      registerSection.setAttribute('aria-hidden', String(!isRegister))
    }

    if (loginSection) {
      const isSignin = mode === 'signin'
      loginSection.hidden = !isSignin
      loginSection.setAttribute('aria-hidden', String(!isSignin))
    }

    updateDocumentTitle(mode)
    syncCardHeight()
  }

  const initialMode = card?.dataset?.mode === 'register' ? 'register' : 'signin';
  setMode(initialMode);
  setupPasswordToggles();

  const signinForm = loginSection?.querySelector('form.auth-form')
  const signinEmailField = signinForm?.querySelector('input[name="username"]')
  const signinPasswordField = signinForm?.querySelector('input[name="password"]')
  const signinFeedback = signinForm?.querySelector('[data-signin-feedback]')
  const signinMessages = {
    missingEmail: signinForm?.dataset?.errorMissingEmail || 'Enter your e-mail.',
    missingPassword: signinForm?.dataset?.errorMissingPassword || 'Enter your password.'
  }

  if (signinFeedback) {
    const hasServerMessage = !signinFeedback.hasAttribute('hidden') && signinFeedback.textContent.trim().length > 0
    signinFeedback.dataset.state = hasServerMessage ? 'server' : 'idle'
  }

  function markSigninInvalid(input, isInvalid) {
    if (!input) {
      return
    }
    const field = input.closest('.auth-field')
    if (field) {
      field.classList.toggle('auth-field--invalid', Boolean(isInvalid))
    }
  }

  function showSigninFeedback(message) {
    if (!signinFeedback) {
      return
    }
    signinFeedback.textContent = message
    signinFeedback.classList.add('auth-feedback--error')
    signinFeedback.removeAttribute('hidden')
    signinFeedback.dataset.state = 'client'
    syncCardHeight()
  }

  function clearSigninFeedback() {
    if (!signinFeedback || signinFeedback.dataset.state === 'server') {
      return
    }
    if (signinFeedback.textContent) {
      signinFeedback.textContent = ''
    }
    if (!signinFeedback.hasAttribute('hidden')) {
      signinFeedback.setAttribute('hidden', '')
      syncCardHeight()
    }
    signinFeedback.dataset.state = 'idle'
  }

  ;[signinEmailField, signinPasswordField].forEach((input) => {
    input?.addEventListener('input', () => {
      markSigninInvalid(input, false)
      clearSigninFeedback()
    })
  })

  if (signinForm) {
    signinForm.addEventListener('submit', (event) => {
      const emailValue = signinEmailField ? signinEmailField.value.trim() : ''
      const passwordValue = signinPasswordField ? signinPasswordField.value : ''
      const missingEmail = !emailValue
      const missingPassword = !passwordValue

      if (missingEmail || missingPassword) {
        event.preventDefault()
        markSigninInvalid(signinEmailField, missingEmail)
        markSigninInvalid(signinPasswordField, missingPassword)
        const message = missingEmail ? signinMessages.missingEmail : signinMessages.missingPassword
        showSigninFeedback(message)
        const focusTarget = missingEmail ? signinEmailField : signinPasswordField
        focusTarget?.focus()
        return
      }

      clearSigninFeedback()
    })
  }

  function setupPasswordToggles() {
    passwordToggleButtons.forEach((button) => {
      const field = button.closest('.auth-field')
      const input = field ? field.querySelector('.auth-field__input') : null
      if (!input) {
        button.disabled = true
        return
      }

      const srOnly = button.querySelector('.sr-only')
      const labelShow = button.dataset.labelShow || 'Show password'
      const labelHide = button.dataset.labelHide || 'Hide password'

      function applyState(isVisible) {
        const pressed = isVisible ? 'true' : 'false'
        button.setAttribute('aria-pressed', pressed)
        const label = isVisible ? labelHide : labelShow
        button.setAttribute('aria-label', label)
        if (srOnly) {
          srOnly.textContent = label
        }
      }

      function updateVisibility() {
        const hasValue = Boolean(input.value)
        button.style.display = hasValue ? 'inline-flex' : 'none'
        if (!hasValue && input.type !== 'password') {
          input.type = 'password'
          applyState(false)
        }
      }

      applyState(false)
      updateVisibility()
      window.requestAnimationFrame(updateVisibility)

      button.addEventListener('click', () => {
        const willShow = input.type === 'password'
        input.type = willShow ? 'text' : 'password'
        applyState(willShow)
        button.style.display = 'inline-flex'
        input.focus()
      })

      input.addEventListener('input', updateVisibility)
      input.addEventListener('blur', updateVisibility)
      input.addEventListener('change', updateVisibility)
    })
  }


  const showRegister = (event) => {
    event.preventDefault()
    setMode('register')
    const focusTarget = registerSection?.querySelector('input[name="email"]')
    focusTarget?.focus()
  }

  const showSignin = (event) => {
    event.preventDefault()
    setMode('signin')
    const focusTarget = loginSection?.querySelector('input[name="username"]')
    focusTarget?.focus()
  }

  root.querySelectorAll('[data-action="show-register"]').forEach((element) => {
    element.addEventListener('click', showRegister)
  })

  root.querySelectorAll('[data-action="show-signin"]').forEach((element) => {
    element.addEventListener('click', showSignin)
  })

  if (!registerForm) {
    return
  }
  let lastSubmittedEmail = null
  let previousBodyOverflow = ''
  const resendUrl = registerForm.dataset.resendUrl || ''
  const homeUrl = registerForm.dataset.homeUrl || ''
  if (successHomeLink && homeUrl) {
    successHomeLink.setAttribute('href', homeUrl)
    successHomeLink.addEventListener('click', (event) => {
      if (event.defaultPrevented) {
        return
      }
      window.location.assign(homeUrl)
    })
  }

  const emailField = registerForm.querySelector('input[name="email"]')
  const passwordField = registerForm.querySelector('input[name="password"]')
  const confirmField = registerForm.querySelector('input[name="confirmPassword"]')
  const feedback = registerForm.querySelector('[data-feedback]')
  const submitButton = registerForm.querySelector('button[type="submit"]')

  const successTemplate = registerForm?.dataset?.successTemplate || ''
  const messages = {
    successFallback: registerForm?.dataset?.successFallback || 'Registration successful. Check your e-mail to activate your account.',
    errorGeneric: registerForm.dataset.errorGeneric || 'We could not create the account. Please try again later.',
    errorNetwork: registerForm.dataset.errorNetwork || 'Network error. Please try again.',
    errorMismatch: registerForm.dataset.errorMismatch || 'Passwords do not match.',
    errorEmpty: registerForm.dataset.errorEmpty || 'Fill in all fields.',
    errorEmail: registerForm.dataset.errorEmail || 'Enter a valid e-mail address.',
    errorPassword: registerForm.dataset.errorPassword || 'Password must be at least 8 characters and include upper and lower case letters, a number, and one of @$!%*?&.',
    resendSuccess: registerForm.dataset.resendSuccess || 'We sent a new verification e-mail.',
    resendError: registerForm.dataset.resendError || 'We could not resend the e-mail. Try again later.',
    resendCooldown: registerForm.dataset.resendCooldown || ''
  }
  function setResendDisabled(isDisabled) {
    if (!successResendButton) {
      return
    }
    successResendButton.setAttribute('aria-disabled', isDisabled ? 'true' : 'false')
    successResendButton.classList.toggle('auth-success__link--disabled', Boolean(isDisabled))
    if (isDisabled) {
      successResendButton.setAttribute('tabindex', '-1')
    } else {
      successResendButton.removeAttribute('tabindex')
    }
  }
  const RESEND_COOLDOWN_PLACEHOLDERS = ['{{time}}', '{{TIME}}', '{{remaining}}', '{{REMAINING}}', '%TIME%', '%time%', '{time}', '{TIME}']

  function cancelResendCooldownTimer() {
    if (resendCooldownTimer) {
      window.clearInterval(resendCooldownTimer)
      resendCooldownTimer = null
    }
  }

  function clearResendCooldown() {
    cancelResendCooldownTimer()
    resendCooldownExpiresAt = 0
    resendCooldownDetail = ''
    setResendDisabled(false)
  }

  function isResendCooldownActive() {
    return resendCooldownExpiresAt > Date.now()
  }

  function formatResendCooldownTime(seconds) {
    const remaining = Math.max(0, Math.ceil(seconds))
    const minutes = Math.floor(remaining / 60)
    const secs = remaining % 60
    const mm = String(minutes).padStart(2, '0')
    const ss = String(secs).padStart(2, '0')
    return `${mm}:${ss}`
  }

  function buildResendCooldownMessage(remainingSeconds) {
    const formatted = formatResendCooldownTime(remainingSeconds)
    const template = messages.resendCooldown || ''
    const token = RESEND_COOLDOWN_PLACEHOLDERS.find((placeholder) => template.includes(placeholder))
    let message
    if (template && token) {
      message = template.split(token).join(formatted)
    } else if (template) {
      message = `${template} ${formatted}`
    } else {
      message = `${messages.resendError} ${formatted}`
    }
    const detail = resendCooldownDetail && resendCooldownDetail.trim()
    if (detail && !message.trim().startsWith(detail)) {
      return `${detail} ${message}`.trim()
    }
    return message.trim()
  }

  function updateResendCooldownFeedback() {
    if (!isResendCooldownActive()) {
      clearResendCooldown()
      clearSuccessFeedback()
      return
    }
    const remaining = Math.ceil((resendCooldownExpiresAt - Date.now()) / 1000)
    if (remaining <= 0) {
      clearResendCooldown()
      clearSuccessFeedback()
      return
    }
    const message = buildResendCooldownMessage(remaining)
    setSuccessFeedback('error', message)
  }

  function startResendCooldown(seconds, detail) {
    const waitSeconds = Math.max(1, Math.ceil(seconds))
    resendCooldownDetail = detail || ''
    cancelResendCooldownTimer()
    resendCooldownExpiresAt = Date.now() + waitSeconds * 1000
    setResendDisabled(true)
    updateResendCooldownFeedback()
    resendCooldownTimer = window.setInterval(updateResendCooldownFeedback, 1000)
  }
  function parseRetryAfterValue(value) {
    if (value === null || value === undefined) {
      return null
    }
    if (typeof value === 'number' && Number.isFinite(value)) {
      return value > 0 ? value : null
    }
    if (typeof value === 'string') {
      const trimmed = value.trim()
      if (!trimmed) {
        return null
      }
      const numeric = Number(trimmed)
      if (!Number.isNaN(numeric)) {
        return numeric > 0 ? numeric : null
      }
      const parsedDate = Date.parse(trimmed)
      if (!Number.isNaN(parsedDate)) {
        const diffSeconds = Math.ceil((parsedDate - Date.now()) / 1000)
        return diffSeconds > 0 ? diffSeconds : null
      }
      return null
    }
    if (Array.isArray(value)) {
      for (let i = 0; i < value.length; i += 1) {
        const parsed = parseRetryAfterValue(value[i])
        if (parsed) {
          return parsed
        }
      }
      return null
    }
    if (typeof value === 'object') {
      const keys = [
        'seconds',
        'value',
        'retryAfter',
        'retry_after',
        'wait',
        'waitSeconds',
        'wait_seconds',
        'cooldown',
        'cooldownSeconds',
        'cooldown_seconds'
      ]
      for (let i = 0; i < keys.length; i += 1) {
        const key = keys[i]
        if (Object.prototype.hasOwnProperty.call(value, key)) {
          const parsed = parseRetryAfterValue(value[key])
          if (parsed) {
            return parsed
          }
        }
      }
    }
    return null
  }

  function extractRetryAfterSecondsFromPayload(payload) {
    if (!payload || typeof payload !== 'object') {
      return null
    }
    const keys = [
      'retryAfterSeconds',
      'retry_after_seconds',
      'retryAfter',
      'retry_after',
      'waitSeconds',
      'wait_seconds',
      'cooldown',
      'cooldownSeconds',
      'cooldown_seconds'
    ]
    for (let i = 0; i < keys.length; i += 1) {
      const key = keys[i]
      if (Object.prototype.hasOwnProperty.call(payload, key)) {
        const parsed = parseRetryAfterValue(payload[key])
        if (parsed) {
          return parsed
        }
      }
    }
    if (payload.meta && typeof payload.meta === 'object') {
      const nested = extractRetryAfterSecondsFromPayload(payload.meta)
      if (nested) {
        return nested
      }
    }
    if (payload.data && typeof payload.data === 'object') {
      const nested = extractRetryAfterSecondsFromPayload(payload.data)
      if (nested) {
        return nested
      }
    }
    return null
  }

  function extractResendErrorDetail(payload) {
    if (!payload || typeof payload !== 'object') {
      return ''
    }
    const candidates = [payload.detail, payload.message, payload.error, payload.reason]
    for (let i = 0; i < candidates.length; i += 1) {
      const candidate = candidates[i]
      if (typeof candidate === 'string') {
        const trimmed = candidate.trim()
        if (trimmed) {
          return trimmed
        }
      }
    }
    if (Array.isArray(payload.errors)) {
      const joined = payload.errors
        .map((item) => (typeof item === 'string' ? item.trim() : ''))
        .filter(Boolean)
        .join(' ')
      if (joined) {
        return joined
      }
    } else if (payload.errors && typeof payload.errors === 'object') {
      const parts = []
      Object.keys(payload.errors).forEach((key) => {
        const value = payload.errors[key]
        if (typeof value === 'string') {
          const trimmed = value.trim()
          if (trimmed) {
            parts.push(trimmed)
          }
        } else if (Array.isArray(value)) {
          value.forEach((item) => {
            if (typeof item === 'string') {
              const trimmed = item.trim()
              if (trimmed) {
                parts.push(trimmed)
              }
            }
          })
        }
      })
      if (parts.length > 0) {
        return parts.join(' ')
      }
    }
    return ''
  }

  function isResendDisabled() {
    return !successResendButton || successResendButton.getAttribute('aria-disabled') === 'true'
  }

  function refreshSuccessResendButton() {
    // Find resend button in both OTP modal and success container
    const nextInOtp = otpModal ? otpModal.querySelector('[data-action="resend-verification"]') : null
    const nextInSuccess = successContainer ? successContainer.querySelector('[data-action="resend-verification"]') : null
    const next = nextInOtp || nextInSuccess

    if (successResendButton === next) {
      return
    }
    if (successResendButton) {
      successResendButton.removeEventListener('click', handleResendVerification)
    }
    successResendButton = next
    if (successResendButton) {
      successResendButton.addEventListener('click', handleResendVerification)
      setResendDisabled(!resendUrl || isResendCooldownActive())
    }
  }

  function buildSuccessMessage(email) {
    if (!successTemplate || !email) {
      return messages.successFallback
    }
    let message = successTemplate
    const placeholders = [/%EMAIL%/g, /{{\\s*email\\s*}}/gi]
    placeholders.forEach((pattern) => {
      message = message.replace(pattern, email)
    })
    return message
  }

  function clearSuccessFeedback() {
    if (!successFeedback) {
      return
    }
    successFeedback.textContent = ''
    successFeedback.classList.remove('auth-success__feedback--error')
  }

  function setSuccessFeedback(type, message) {
    if (!successFeedback) {
      return
    }
    successFeedback.textContent = message || ''
    successFeedback.classList.remove('auth-success__feedback--error')
    if (type === 'error') {
      successFeedback.classList.add('auth-success__feedback--error')
    }
  }

  function showSuccessView(message) {
    if (!successContainer) {
      return
    }
    setRegisterState('success')
    clearResendCooldown()
    if (successMessage) {
      successMessage.innerHTML = message
    }
    refreshSuccessResendButton()
    clearSuccessFeedback()
    if (registerForm) {
      registerForm.hidden = true
      registerForm.setAttribute('aria-hidden', 'true')
      registerForm.style.display = 'none'
    }

    if (otpModal) {
      otpModal.hidden = false
      otpModal.setAttribute('aria-hidden', 'false')
      otpFeedback && (otpFeedback.textContent = '')
      window.requestAnimationFrame(() => otpInput?.focus())
    } else {
      successContainer.hidden = false
      successContainer.setAttribute('aria-hidden', 'false')
    }
    syncCardHeight()
    window.requestAnimationFrame(() => {
      if (!isResendDisabled()) {
        successResendButton?.focus()
      } else if (successHomeLink) {
        successHomeLink.focus()
      }
    })
  }

  async function submitOtp() {
    if (!otpInput) return
    const raw = otpInput.value.trim()
    if (!/^\d{8}$/.test(raw)) {
      if (otpFeedback) {
        otpFeedback.textContent = registerForm?.dataset?.otpInvalid || 'Enter a valid 8-digit code.'
        otpFeedback.classList.add('auth-success__feedback--error')
      }
      return
    }
    try {
      const resp = await fetch('/api/auth/verify-code', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json', Accept: 'application/json' },
        body: JSON.stringify({ email: lastSubmittedEmail, code: raw })
      })
      const payload = await resp.json().catch(() => null)
      if (!resp.ok) {
        const status = payload && typeof payload.status === 'string' ? payload.status.toUpperCase() : ''

        if (status === 'TOO_MANY_ATTEMPTS') {
          const msg = (payload && payload.detail) || 'Too many incorrect attempts. Please request a new code.'
          if (otpFeedback) {
            otpFeedback.textContent = msg
            otpFeedback.classList.add('auth-success__feedback--error')
          }
          if (otpInput) {
            otpInput.value = ''
          }
          return
        }

        const msg = (payload && (payload.detail || payload.message)) || 'Invalid code.'
        if (otpFeedback) {
          otpFeedback.textContent = msg
          otpFeedback.classList.add('auth-success__feedback--error')
        }
        return
      }

      const status = payload && typeof payload.status === 'string' ? payload.status.toUpperCase() : 'VERIFIED'
      if (status !== 'VERIFIED') {
        if (otpFeedback) {
          otpFeedback.textContent = 'Verification failed. Please request a new code.'
          otpFeedback.classList.add('auth-success__feedback--error')
        }
        return
      }

      if (otpModal) {
        otpModal.hidden = true
        otpModal.setAttribute('aria-hidden', 'true')
      }
      if (successContainer) {
        successContainer.hidden = false
        successContainer.setAttribute('aria-hidden', 'false')
        const msgEl = successContainer.querySelector('[data-success-message]')
        if (msgEl) msgEl.textContent = registerForm?.dataset?.successOtp || (registerForm?.dataset?.successOtpText || '') || 'Registration confirmed successfully.'
      }
      syncCardHeight()
    } catch (e) {
      if (otpFeedback) {
        otpFeedback.textContent = 'Network error. Try again.'
        otpFeedback.classList.add('auth-success__feedback--error')
      }
    }
  }

  registerSection?.querySelector('[data-action="submit-otp"]').addEventListener('click', submitOtp)

  function showRegisterForm() {
    setRegisterState('form')
    clearResendCooldown()
    if (registerForm) {
      registerForm.hidden = false
      registerForm.setAttribute('aria-hidden', 'false')
      if (registerForm.style) {
        registerForm.style.removeProperty('display')
      }
    }
    if (successContainer) {
      successContainer.hidden = true
      successContainer.setAttribute('aria-hidden', 'true')
    }
    clearSuccessFeedback()
    syncCardHeight()
  }

  async function handleResendVerification(event) {
    if (event) {
      event.preventDefault()
    }
    if (!successResendButton) {
      return
    }
    if (!resendUrl || !lastSubmittedEmail) {
      setSuccessFeedback('error', messages.resendError)
      return
    }
    if (isResendDisabled()) {
      return
    }

    setResendDisabled(true)
    clearSuccessFeedback()

    let waitSeconds = null
    let detailMessage = ''
    try {
      const response = await fetch(resendUrl, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          Accept: 'application/json'
        },
        body: JSON.stringify({ email: lastSubmittedEmail, purpose: 'REGISTRATION' })
      })

      if (!response.ok) {
        const payload = await response.json().catch(() => null)
        detailMessage = extractResendErrorDetail(payload)
        const retryAfterHeader = response.headers && typeof response.headers.get === 'function'
          ? response.headers.get('Retry-After')
          : null
        const headerRetryAfter = parseRetryAfterValue(retryAfterHeader)
        const payloadRetryAfter = extractRetryAfterSecondsFromPayload(payload)
        if (typeof payloadRetryAfter === 'number' && Number.isFinite(payloadRetryAfter)) {
          waitSeconds = payloadRetryAfter
        } else if (typeof headerRetryAfter === 'number' && Number.isFinite(headerRetryAfter)) {
          waitSeconds = headerRetryAfter
        } else {
          waitSeconds = null
        }
        const error = new Error(detailMessage || messages.resendError)
        if (waitSeconds) {
          error.waitSeconds = waitSeconds
        }
        if (detailMessage) {
          error.detail = detailMessage
        }
        throw error
      }

      clearResendCooldown()
      setSuccessFeedback('success', messages.resendSuccess)
    } catch (error) {
      const waitFromError = error && typeof error.waitSeconds === 'number' ? error.waitSeconds : null
      const parsedWait = typeof waitFromError === 'number' && Number.isFinite(waitFromError)
        ? Math.ceil(waitFromError)
        : (typeof waitSeconds === 'number' && Number.isFinite(waitSeconds) ? Math.ceil(waitSeconds) : null)
      const detailFromError = error && typeof error.detail === 'string' && error.detail.trim()
        ? error.detail.trim()
        : ''
      const normalizedDetail = detailFromError || detailMessage || ''
      if (parsedWait && parsedWait > 0) {
        startResendCooldown(parsedWait, normalizedDetail)
      } else {
        let fallbackMessage = normalizedDetail
        if (!fallbackMessage && error && typeof error.message === 'string' && error.message && error.message !== 'Failed to resend') {
          fallbackMessage = error.message
        }
        if (!fallbackMessage) {
          fallbackMessage = messages.resendError
        }
        setSuccessFeedback('error', fallbackMessage)
      }
    } finally {
      if (!isResendCooldownActive()) {
        setResendDisabled(false)
      }
    }
  }
  const emailPattern = /^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$/
  const passwordPattern = /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[@$!%*?&])[A-Za-z\d@$!%*?&]{8,}$/

  function isPasswordStrong(value) {
    return passwordPattern.test(value)
  }

  function markInvalid(input, isInvalid) {
    if (!input) {
      return
    }
    const field = input.closest('.auth-field')
    if (field) {
      field.classList.toggle('auth-field--invalid', Boolean(isInvalid))
    }
  }

  function resetValidationState() {
    markInvalid(emailField, false)
    markInvalid(passwordField, false)
    markInvalid(confirmField, false)
  }

  function setFeedback(type, message) {
    if (!feedback) {
      return
    }
    feedback.textContent = message
    feedback.classList.remove('auth-feedback--error', 'auth-feedback--success')
    if (type === 'error') {
      feedback.classList.add('auth-feedback--error')
    } else if (type === 'success') {
      feedback.classList.add('auth-feedback--success')
    }
    syncCardHeight()
  }

  function toggleLoading(isLoading) {
    if (submitButton) {
      submitButton.disabled = isLoading
    }
  }

  function clearFeedbackIfNeeded() {
    if (feedback && feedback.classList.contains('auth-feedback--error')) {
      setFeedback('info', '')
    }
  }

  ;[emailField, passwordField, confirmField].forEach((input) => {
    input?.addEventListener('input', () => {
      markInvalid(input, false)
      clearFeedbackIfNeeded()
    })
  })

  function resetPasswordToggles() {
    passwordToggleButtons.forEach((button) => {
      const field = button.closest('.auth-field')
      const input = field ? field.querySelector('.auth-field__input') : null
      if (!input) {
        return
      }
      button.style.display = 'none'
      button.setAttribute('aria-pressed', 'false')
      const label = button.dataset.labelShow || 'Show password'
      button.setAttribute('aria-label', label)
      const srOnly = button.querySelector('.sr-only')
      if (srOnly) {
        srOnly.textContent = label
      }
      input.type = 'password'
    })
  }


  refreshSuccessResendButton()

  async function handleSubmit(event) {
    event.preventDefault()

    const email = emailField ? emailField.value.trim() : ''
    const password = passwordField ? passwordField.value : ''
    const confirm = confirmField ? confirmField.value : ''

    resetValidationState()

    const missingFields = []
    if (!email) { missingFields.push(emailField) }
    if (!password) { missingFields.push(passwordField) }
    if (!confirm) { missingFields.push(confirmField) }

    if (missingFields.length > 0) {
      missingFields.forEach((field) => markInvalid(field, true))
      setFeedback('error', messages.errorEmpty)
      return
    }

    if (!emailPattern.test(email)) {
      markInvalid(emailField, true)
      setFeedback('error', messages.errorEmail)
      return
    }

    if (!isPasswordStrong(password)) {
      markInvalid(passwordField, true)
      setFeedback('error', messages.errorPassword)
      return
    }

    if (password !== confirm) {
      markInvalid(passwordField, true)
      markInvalid(confirmField, true)
      setFeedback('error', messages.errorMismatch)
      return
    }

    setFeedback('info', '')
    toggleLoading(true)

    try {
      const response = await fetch('/api/auth/register', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          Accept: 'application/json'
        },
        body: JSON.stringify({ email, password })
      })

      if (!response.ok) {
        const payload = await response.json().catch(() => null)
        const message = (payload && (payload.detail || payload.message)) || messages.errorGeneric
        setFeedback('error', message)
        return
      }

      const successMessage = buildSuccessMessage(email)
      lastSubmittedEmail = email
      registerForm.reset()
      resetValidationState()
      resetPasswordToggles()
      setFeedback('info', '')
      showSuccessView(successMessage)
    } catch (error) {
      setFeedback('error', messages.errorNetwork)
    } finally {
      toggleLoading(false)
    }
  }

  registerForm.addEventListener('submit', handleSubmit)

  // =============================================================================
  // Password Reset Flow
  // =============================================================================

  const forgotPasswordModal = loginSection?.querySelector('[data-forgot-password-modal]')
  const resetOtpModal = loginSection?.querySelector('[data-reset-otp-modal]')
  const resetPasswordModal = loginSection?.querySelector('[data-reset-password-modal]')
  const resetSuccessModal = loginSection?.querySelector('[data-reset-success-modal]')

  const resetEmailInput = forgotPasswordModal?.querySelector('[data-reset-email-input]')
  const resetOtpInput = resetOtpModal?.querySelector('[data-reset-otp-input]')
  const newPasswordInput = resetPasswordModal?.querySelector('[data-new-password-input]')
  const confirmPasswordInput = resetPasswordModal?.querySelector('[data-confirm-password-input]')

  const forgotFeedback = forgotPasswordModal?.querySelector('[data-forgot-feedback]')
  const resetOtpFeedback = resetOtpModal?.querySelector('[data-reset-otp-feedback]')
  const resetPasswordFeedback = resetPasswordModal?.querySelector('[data-reset-password-feedback]')

  let resetEmail = ''
  let resetToken = ''

  function hideAllResetModals() {
    ;[forgotPasswordModal, resetOtpModal, resetPasswordModal, resetSuccessModal].forEach(modal => {
      if (modal) {
        modal.hidden = true
        modal.setAttribute('aria-hidden', 'true')
      }
    })
  }

  function showResetModal(modal) {
    hideAllResetModals()
    if (modal) {
      modal.hidden = false
      modal.setAttribute('aria-hidden', 'false')
      syncCardHeight()
    }
  }

  function setResetFeedback(feedbackEl, message, isError = false) {
    if (!feedbackEl) return
    feedbackEl.textContent = message
    if (isError) {
      feedbackEl.classList.add('auth-success__feedback--error')
    } else {
      feedbackEl.classList.remove('auth-success__feedback--error')
    }
  }

  function clearResetFeedback(feedbackEl) {
    if (!feedbackEl) return
    feedbackEl.textContent = ''
    feedbackEl.classList.remove('auth-success__feedback--error')
  }

  // Step 1: Show forgot password modal
  root.querySelectorAll('[data-action="show-forgot-password"]').forEach(element => {
    element.addEventListener('click', (event) => {
      event.preventDefault()
      showResetModal(forgotPasswordModal)
      resetEmailInput?.focus()
      clearResetFeedback(forgotFeedback)
      if (resetEmailInput) resetEmailInput.value = ''
      resetToken = ''
    })
  })

  // Step 1: Send reset code
  forgotPasswordModal?.querySelector('[data-action="send-reset-code"]')?.addEventListener('click', async () => {
    const email = resetEmailInput?.value?.trim() || ''

    if (!email || !emailPattern.test(email)) {
      setResetFeedback(forgotFeedback, 'Please enter a valid email address.', true)
      return
    }

    clearResetFeedback(forgotFeedback)
    resetEmail = email
    resetToken = ''

    try {
      const response = await fetch('/api/auth/forgot-password', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json', Accept: 'application/json' },
        body: JSON.stringify({ email, purpose: 'PASSWORD_RESET' })
      })

      if (!response.ok) {
        setResetFeedback(forgotFeedback, 'Could not send reset code. Please try again.', true)
        return
      }

      // Move to OTP modal
      showResetModal(resetOtpModal)
      if (resetOtpInput) resetOtpInput.value = ''
      clearResetFeedback(resetOtpFeedback)
      resetOtpInput?.focus()
    } catch (error) {
      setResetFeedback(forgotFeedback, 'Network error. Please try again.', true)
    }
  })

  // Step 1: Cancel forgot password
  forgotPasswordModal?.querySelector('[data-action="cancel-forgot"]')?.addEventListener('click', () => {
    hideAllResetModals()
    syncCardHeight()
  })

  // Step 2: Verify reset code
  resetOtpModal?.querySelector('[data-action="verify-reset-code"]')?.addEventListener('click', async () => {
    const code = resetOtpInput?.value?.trim() || ''

    if (!code || !/^\d{8}$/.test(code)) {
      setResetFeedback(resetOtpFeedback, 'Enter a valid 8-digit code.', true)
      return
    }

    clearResetFeedback(resetOtpFeedback)

    try {
      const response = await fetch('/api/auth/verify-code', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json', Accept: 'application/json' },
        body: JSON.stringify({ email: resetEmail, code, purpose: 'PASSWORD_RESET' })
      })

      const payload = await response.json().catch(() => null)

      if (!response.ok) {
        resetToken = ''
        const message = (payload && payload.detail) || 'Invalid code. Please try again.'
        setResetFeedback(resetOtpFeedback, message, true)
        return
      }

      const status = payload && typeof payload.status === 'string' ? payload.status.toUpperCase() : ''
      if (status !== 'VERIFIED') {
        resetToken = ''
        setResetFeedback(resetOtpFeedback, 'Verification failed. Please request a new code.', true)
        return
      }

      resetToken = payload && typeof payload.resetToken === 'string' ? payload.resetToken : ''
      if (!resetToken) {
        setResetFeedback(resetOtpFeedback, 'Verification token missing. Please request a new code.', true)
        return
      }

      showResetModal(resetPasswordModal)
      if (newPasswordInput) newPasswordInput.value = ''
      if (confirmPasswordInput) confirmPasswordInput.value = ''
      clearResetFeedback(resetPasswordFeedback)
      newPasswordInput?.focus()
    } catch (error) {
      setResetFeedback(resetOtpFeedback, 'Network error. Please try again.', true)
    }
  })

  // Step 2: Back to forgot password
  resetOtpModal?.querySelector('[data-action="back-to-forgot"]')?.addEventListener('click', () => {
    showResetModal(forgotPasswordModal)
    resetEmailInput?.focus()
    resetToken = ''
  })

  // Step 3: Complete password reset
  resetPasswordModal?.querySelector('[data-action="complete-reset"]')?.addEventListener('click', async () => {
    const newPassword = newPasswordInput?.value || ''
    const confirmPassword = confirmPasswordInput?.value || ''

    if (!newPassword || !confirmPassword) {
      setResetFeedback(resetPasswordFeedback, 'All fields are required.', true)
      return
    }

    if (newPassword !== confirmPassword) {
      setResetFeedback(resetPasswordFeedback, 'Passwords do not match.', true)
      return
    }

    if (!isPasswordStrong(newPassword)) {
      setResetFeedback(resetPasswordFeedback, 'Password must be at least 8 characters and include upper and lower case letters, a number, and one of @$!%*?&.', true)
      return
    }

    clearResetFeedback(resetPasswordFeedback)

    if (!resetToken) {
      setResetFeedback(resetPasswordFeedback, 'Verification expired. Please request a new code.', true)
      return
    }

    try {
      const response = await fetch('/api/auth/reset-password', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json', Accept: 'application/json' },
        body: JSON.stringify({ email: resetEmail, password: newPassword, confirmPassword, resetToken })
      })

      if (!response.ok) {
        const payload = await response.json().catch(() => null)
        const message = (payload && payload.detail) || 'Could not reset password. Please try again.'
        setResetFeedback(resetPasswordFeedback, message, true)
        return
      }

      // Show success modal
      showResetModal(resetSuccessModal)
      resetToken = ''
    } catch (error) {
      setResetFeedback(resetPasswordFeedback, 'Network error. Please try again.', true)
    }
  })

  // Step 4: Go to sign in after success
  resetSuccessModal?.querySelector('[data-action="show-signin"]')?.addEventListener('click', (event) => {
    event.preventDefault()
    hideAllResetModals()
    setMode('signin')
    syncCardHeight()
    const focusTarget = loginSection?.querySelector('input[name="username"]')
    focusTarget?.focus()
  })

})()





