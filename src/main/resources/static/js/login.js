(function () {
  const root = document.querySelector('[data-auth-root]')
  if (!root) {
    return
  }

  const card = root.querySelector('.auth-card')
  const signinView = root.querySelector('[data-view="signin"]')
  const registerView = root.querySelector('[data-view="register"]')
  const signinForm = root.querySelector('[data-signin-form]')
  const registerForm = root.querySelector('[data-register-form]')
  const registerError = root.querySelector('[data-register-error]')
  const signinError = root.querySelector('[data-signin-error]')
  const titleStore = {
    signin: card?.dataset?.signinTitle || document.title,
    register: card?.dataset?.registerTitle || document.title
  }
  const defaultResendCooldown = Number(card?.dataset?.resendCooldown) || 0

  const i18nStore = document.querySelector('[data-i18n-store]')
  const messages = i18nStore ? { ...i18nStore.dataset } : {}

  const PASSWORD_REGEX = /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[@$!%*?&])[A-Za-z\d@$!%*?&]{8,}$/
  const EMAIL_REGEX = /^[^\s@]+@[^\s@]+\.[^\s@]+$/

  const otpModal = document.querySelector('[data-modal="otp"]')
  const otpInput = otpModal?.querySelector('[data-otp-input]')
  const otpMessage = otpModal?.querySelector('[data-otp-message]')
  const otpDescription = otpModal?.querySelector('[data-otp-description]')
  const otpResendButton = otpModal?.querySelector('[data-action="otp-resend"]')

  const registerSuccessModal = document.querySelector('[data-modal="register-success"]')
  const forgotEmailModal = document.querySelector('[data-modal="forgot-email"]')
  const forgotEmailInput = forgotEmailModal?.querySelector('[data-forgot-email-input]')
  const forgotEmailMessage = forgotEmailModal?.querySelector('[data-forgot-email-message]')

  const forgotCodeModal = document.querySelector('[data-modal="forgot-code"]')
  const forgotCodeInput = forgotCodeModal?.querySelector('[data-forgot-code-input]')
  const forgotCodeMessage = forgotCodeModal?.querySelector('[data-forgot-code-message]')
  const forgotCodeDescription = forgotCodeModal?.querySelector('[data-forgot-code-description]')
  const forgotResendButton = forgotCodeModal?.querySelector('[data-action="forgot-resend"]')

  const forgotResetModal = document.querySelector('[data-modal="forgot-reset"]')
  const forgotPasswordInput = forgotResetModal?.querySelector('[data-forgot-password-input]')
  const forgotConfirmInput = forgotResetModal?.querySelector('[data-forgot-confirm-input]')
  const forgotResetMessage = forgotResetModal?.querySelector('[data-forgot-reset-message]')

  const forgotSuccessModal = document.querySelector('[data-modal="forgot-success"]')

  let activeModal = null
  let registerEmail = ''
  let otpEmail = ''
  let forgotEmail = ''
  let resetToken = ''
  let resendTimer = null

  function getMessage(key, fallback = '') {
    return messages && typeof messages[key] === 'string' ? messages[key] : fallback
  }

  function format(template, value) {
    if (typeof template !== 'string') {
      return ''
    }
    return template.replace('{email}', value || '')
  }

  function isPasswordStrong(value) {
    return PASSWORD_REGEX.test(value || '')
  }

  function isEmailValid(value) {
    return EMAIL_REGEX.test(value || '')
  }

  // Remove error query param from URL and hide server-rendered error message
  function removeErrorParam() {
    try {
      const url = new URL(window.location.href)
      if (url.searchParams.has('error')) {
        url.searchParams.delete('error')
        const search = url.searchParams.toString()
        const newUrl = url.pathname + (search ? `?${search}` : '') + url.hash
        window.history.replaceState({}, document.title, newUrl)
      }
      clearMessage(signinError)
    } catch (_) {}
  }

  function openModal(modal) {
    if (!modal) {
      return
    }
    closeModal(activeModal)
    modal.hidden = false
    modal.setAttribute('aria-hidden', 'false')
    document.body.classList.add('auth-modal-open')
    activeModal = modal
    const focusable = modal.querySelector('input, button, a')
    if (focusable) {
      window.setTimeout(() => focusable.focus(), 50)
    }
  }

  function closeModal(modal) {
    if (!modal) {
      return
    }
    modal.hidden = true
    modal.setAttribute('aria-hidden', 'true')
    if (activeModal === modal) {
      activeModal = null
      document.body.classList.remove('auth-modal-open')
    }
  }

  function closeAllModals() {
    document.querySelectorAll('.auth-modal').forEach(closeModal)
  }

  document.querySelectorAll('[data-action="close-modal"]').forEach((button) => {
    button.addEventListener('click', () => closeModal(button.closest('.auth-modal')))
  })

  registerSuccessModal?.querySelector('[data-action="close-modal"]')?.addEventListener('click', () => {
    removeErrorParam()
    setMode('signin')
  })

  forgotSuccessModal?.querySelector('[data-action="close-modal"]')?.addEventListener('click', () => {
    setMode('signin')
  })

  document.querySelectorAll('[data-action="go-signin"]').forEach((button) => {
    button.addEventListener('click', () => {
      closeAllModals()
      removeErrorParam()
      setMode('signin')
      signinForm?.querySelector('input[name="username"]')?.focus()
    })
  })

  document.querySelectorAll('[data-action="go-home"]').forEach((link) => {
    link.addEventListener('click', () => closeAllModals())
  })

  function setMode(mode) {
    if (!card || (mode !== 'signin' && mode !== 'register')) {
      return
    }
    card.dataset.mode = mode
    if (mode === 'signin') {
      registerView?.setAttribute('hidden', 'true')
      registerView?.setAttribute('aria-hidden', 'true')
      signinView?.removeAttribute('hidden')
      signinView?.setAttribute('aria-hidden', 'false')
    } else {
      signinView?.setAttribute('hidden', 'true')
      signinView?.setAttribute('aria-hidden', 'true')
      registerView?.removeAttribute('hidden')
      registerView?.setAttribute('aria-hidden', 'false')
    }
    if (titleStore[mode]) {
      document.title = titleStore[mode]
    }
  }

  root.querySelectorAll('[data-action="show-register"]').forEach((button) => {
    button.addEventListener('click', () => {
      setMode('register')
      registerForm?.querySelector('input[name="email"]')?.focus()
    })
  })

  root.querySelectorAll('[data-action="show-signin"]').forEach((button) => {
    button.addEventListener('click', () => {
      setMode('signin')
      signinForm?.querySelector('input[name="username"]')?.focus()
    })
  })

  const forgotTriggers = root.querySelectorAll('[data-action="open-forgot"]')
  forgotTriggers.forEach((trigger) => {
    trigger.addEventListener('click', () => {
      clearMessage(forgotEmailMessage)
      forgotEmailInput && (forgotEmailInput.value = '')
      openModal(forgotEmailModal)
    })
  })

  function togglePasswordVisibility(button) {
    const input = button?.parentElement?.querySelector('input')
    if (!input) return
    const showLabel = button.dataset.labelShow || 'Show'
    const hideLabel = button.dataset.labelHide || 'Hide'
    const isPassword = input.type === 'password'
    input.type = isPassword ? 'text' : 'password'
    button.setAttribute('aria-label', isPassword ? hideLabel : showLabel)
    const toggleIcon = button.querySelector('.auth-field__toggle-icon')
    if (toggleIcon) {
      toggleIcon.classList.toggle('auth-field__toggle-icon--active', isPassword)
    }
  }

  // Sign-in client-side validation
  if (signinForm) {
    signinForm.addEventListener('submit', (event) => {
      const emailInput = signinForm.querySelector('input[name="username"]')
      const passwordInput = signinForm.querySelector('input[name="password"]')
      const email = emailInput?.value?.trim() || ''
      const password = passwordInput?.value || ''

      // All fields empty or one empty → single fill-all message
      if (!email || !password) {
        const fillMsg = signinForm.dataset.errorFill || signinForm.dataset.errorEmail || 'Fill all fields.'
        showMessage(signinError, fillMsg)
        if (!email) {
          emailInput?.focus()
        } else {
          passwordInput?.focus()
        }
        event.preventDefault()
        return
      }

      // Invalid email format
      if (!isEmailValid(email)) {
        const msg = signinForm.dataset.errorEmail || 'Enter a valid e-mail address.'
        showMessage(signinError, msg)
        emailInput?.focus()
        event.preventDefault()
      }
    })
  }

  const passwordToggles = Array.from(root.querySelectorAll('[data-toggle-password]'))

  passwordToggles.forEach((button) => {
    const input = button?.parentElement?.querySelector('input')
    if (!input) {
      button.classList.add('auth-field__toggle--hidden')
      return
    }

    const updateToggleVisibility = () => {
      const hasValue = Boolean(input.value && input.value.length > 0)
      const focused = document.activeElement === input
      // Показать «глаз» только когда поле в фокусе и есть значение
      button.classList.toggle('auth-field__toggle--hidden', !(focused && hasValue))
    }

    input.addEventListener('input', updateToggleVisibility)
    input.addEventListener('change', updateToggleVisibility)
    input.addEventListener('focus', updateToggleVisibility)
    input.addEventListener('blur', updateToggleVisibility)
    updateToggleVisibility()

    button.addEventListener('mousedown', (e) => e.preventDefault())
    button.addEventListener('click', (event) => {
      event.preventDefault()
      togglePasswordVisibility(button)
      input.focus()
      updateToggleVisibility()
    })
  })

  const messageTimers = new WeakMap()

  function showMessage(container, text, type = 'error') {
    if (!container) return
    const existingTimer = messageTimers.get(container)
    if (existingTimer) {
      window.clearTimeout(existingTimer)
      messageTimers.delete(container)
    }
    container.textContent = text
    container.hidden = false
    container.setAttribute('aria-hidden', 'false')
    container.classList.remove('auth-message--error', 'auth-message--success')
    container.classList.add(type === 'success' ? 'auth-message--success' : 'auth-message--error')

    if (type === 'success') {
      const timeoutId = window.setTimeout(() => {
        clearMessage(container)
      }, 5000)
      messageTimers.set(container, timeoutId)
    }
  }

  function clearMessage(container) {
    if (!container) return
    const existingTimer = messageTimers.get(container)
    if (existingTimer) {
      window.clearTimeout(existingTimer)
      messageTimers.delete(container)
    }
    container.textContent = ''
    container.hidden = true
    container.setAttribute('aria-hidden', 'true')
    container.classList.remove('auth-message--error', 'auth-message--success')
  }

  async function fetchJson(url, options = {}) {
    const response = await fetch(url, {
      headers: { 'Content-Type': 'application/json', Accept: 'application/json', ...(options.headers || {}) },
      ...options
    })
    let payload = null
    if (response.status !== 204) {
      payload = await response.json().catch(() => null)
    }
    return { response, payload }
  }

  function markResendPending(button) {
    if (!button) return
    button.dataset.resendBusy = 'true'
    button.disabled = true
    button.classList.add('auth-otp-resend--disabled')
  }

  function resetResendButton(button) {
    if (!button) return
    delete button.dataset.resendBusy
    const originalLabel = button.dataset.label || button.dataset.originalText || button.textContent || ''
    button.disabled = false
    button.textContent = originalLabel
    button.classList.remove('auth-otp-resend--disabled')
  }

  function startCooldown(button, seconds) {
    if (!button) return
    delete button.dataset.resendBusy
    let remaining = Math.max(Math.round(Number(seconds) || 0), 0)
    const originalLabel = button.dataset.label || button.dataset.originalText || button.textContent || ''
    const countdownTemplate = button.dataset.countdown || `${originalLabel} ({0})`
    button.dataset.originalText = originalLabel

    if (resendTimer) {
      window.clearInterval(resendTimer)
      resendTimer = null
    }

    const tick = () => {
      if (remaining <= 0) {
        button.disabled = false
        button.textContent = originalLabel
        button.classList.remove('auth-otp-resend--disabled')
        return false
      }
      button.disabled = true
      button.textContent = countdownTemplate.replace('{0}', formatCooldown(remaining))
      button.classList.add('auth-otp-resend--disabled')
      remaining -= 1
      return true
    }

    if (!tick()) {
      return
    }

    resendTimer = window.setInterval(() => {
      if (!tick()) {
        window.clearInterval(resendTimer)
        resendTimer = null
      }
    }, 1000)
  }

  function extractCooldownSeconds(payload) {
    if (!payload || typeof payload !== 'object') {
      return defaultResendCooldown
    }

    const candidates = []
    const rawSeconds = payload.cooldownSeconds ?? payload.retryAfterSeconds
    if (rawSeconds !== undefined && rawSeconds !== null && rawSeconds !== '') {
      const parsed = Number(rawSeconds)
      if (Number.isFinite(parsed)) {
        candidates.push(parsed)
      }
    }

    if (payload.cooldownUntil) {
      const parsedUntil = Date.parse(payload.cooldownUntil)
      if (!Number.isNaN(parsedUntil)) {
        const diff = Math.ceil((parsedUntil - Date.now()) / 1000)
        if (Number.isFinite(diff)) {
          candidates.push(diff)
        }
      }
    }

    if (candidates.length === 0) {
      return defaultResendCooldown
    }

    return Math.max(0, Math.max(...candidates))
  }

  function resolveCooldownSeconds(payload, fallbackHeader) {
    const fromPayload = extractCooldownSeconds(payload)
    if (fromPayload > 0) {
      return fromPayload
    }
    const fromHeader = Number(fallbackHeader)
    if (Number.isFinite(fromHeader) && fromHeader > 0) {
      return fromHeader
    }
    return 0
  }

  function applyCooldownFromPayload(button, payload, fallbackHeader) {
    if (!button) return
    const seconds = resolveCooldownSeconds(payload, fallbackHeader)
    startCooldown(button, seconds)
  }

  function formatCooldown(seconds) {
    const total = Math.max(0, Math.round(seconds))
    const minutes = Math.floor(total / 60)
    const secs = total % 60
    if (minutes > 0) {
      return `${minutes}:${String(secs).padStart(2, '0')} sec`
    }
    return `${secs} sec`
  }

  if (registerForm) {
    registerForm.addEventListener('submit', async (event) => {
      event.preventDefault()
      clearMessage(registerError)

      const emailInput = registerForm.querySelector('input[name="email"]')
      const passwordInput = registerForm.querySelector('input[name="password"]')
      const confirmInput = registerForm.querySelector('input[name="confirmPassword"]')
      const submitButton = registerForm.querySelector('[type="submit"]')

      const email = emailInput?.value?.trim() || ''
      const password = passwordInput?.value || ''
      const confirmPassword = confirmInput?.value || ''

      if (!email || !password || !confirmPassword) {
        const fillMsg =
          registerForm.dataset.errorFill ||
          registerForm.dataset.errorEmail ||
          'Fill all fields.'
        showMessage(registerError, fillMsg)
        if (!email) {
          emailInput?.focus()
        } else if (!password) {
          passwordInput?.focus()
        } else {
          confirmInput?.focus()
        }
        return
      }
      if (!isEmailValid(email)) {
        showMessage(registerError, registerForm.dataset.errorEmail || getMessage('register.errors.email', 'Enter a valid e-mail.'))
        emailInput?.focus()
        return
      }

      if (password !== confirmPassword) {
        showMessage(registerError, registerForm.dataset.errorMismatch || 'Passwords do not match.')
        confirmInput?.focus()
        return
      }

      if (!isPasswordStrong(password)) {
        showMessage(registerError, registerForm.dataset.errorPassword || 'Password does not meet requirements.')
        passwordInput?.focus()
        return
      }

      try {
        submitButton?.setAttribute('disabled', 'disabled')
        const { response, payload } = await fetchJson('/api/auth/register', {
          method: 'POST',
          body: JSON.stringify({ email, password, confirmPassword })
        })

        if (!response.ok) {
          const errors = payload?.errors || {}
          const errorCode = payload?.errorCode
          if (errors.email === 'EMAIL_INVALID' || errors.email === 'EMAIL_REQUIRED' || errorCode === 'EMAIL_INVALID' || errorCode === 'EMAIL_REQUIRED') {
            showMessage(registerError, registerForm.dataset.errorEmail || getMessage('register.errors.email', 'Enter a valid e-mail.'))
            emailInput?.focus()
            return
          }
          if (errors.password === 'PASSWORD_WEAK' || errors.password === 'PASSWORD_REQUIRED' || errorCode === 'PASSWORD_WEAK' || errorCode === 'PASSWORD_REQUIRED') {
            showMessage(registerError, registerForm.dataset.errorPassword || getMessage('register.errors.password', 'Password does not meet requirements.'))
            passwordInput?.focus()
            return
          }
          if (errors.confirmPassword === 'PASSWORDS_DO_NOT_MATCH' || errorCode === 'PASSWORDS_DO_NOT_MATCH') {
            showMessage(registerError, registerForm.dataset.errorMismatch || getMessage('register.errors.mismatch', 'Passwords do not match.'))
            confirmInput?.focus()
            return
          }
          if (errors.confirmPassword === 'PASSWORD_WEAK' || errors.confirmPassword === 'PASSWORD_REQUIRED') {
            showMessage(registerError, registerForm.dataset.errorPassword || getMessage('register.errors.password', 'Password does not meet requirements.'))
            confirmInput?.focus()
            return
          }
          const message = (payload && payload.detail) || registerForm.dataset.errorGeneric || getMessage('genericError', 'Could not create the account.')
          showMessage(registerError, message)
          return
        }

        registerEmail = email
        otpEmail = email

        if (otpModal) {
          if (otpDescription) {
            const template = otpDescription.dataset.template || otpDescription.textContent || ''
            otpDescription.textContent = format(template, registerEmail)
          }
          if (otpInput) {
            otpInput.value = ''
          }
          clearMessage(otpMessage)
          openModal(otpModal)
          if (otpResendButton) {
            markResendPending(otpResendButton)
            applyCooldownFromPayload(otpResendButton, payload, response.headers.get('Retry-After'))
          }
        }
      } catch (error) {
        showMessage(registerError, registerForm.dataset.errorGeneric || getMessage('genericError', 'Something went wrong. Try again later.'))
      } finally {
        submitButton?.removeAttribute('disabled')
      }
    })
  }

  if (otpModal) {
    otpInput?.addEventListener('input', () => clearMessage(otpMessage))

    otpModal.querySelector('[data-action="otp-submit"]')?.addEventListener('click', async () => {
      const code = otpInput?.value?.trim() || ''
      if (!code || !/^\d{8}$/.test(code)) {
        showMessage(otpMessage, getMessage('otpError', 'Enter a valid code.'))
        return
      }

      // Disabled-login flow: always use email from sign-in field; otherwise use register email
      const params = new URLSearchParams(window.location.search || '')
      const isDisabled = params.get('error') === 'disabled'
      const emailToUse = isDisabled ? (otpEmail || '') : (registerEmail || '')
      if (!emailToUse) {
        showMessage(otpMessage, getMessage('otpError', 'Registration session expired.'))
        return
      }

      clearMessage(otpMessage)

      try {
        const { response, payload } = await fetchJson('/api/auth/verify-code', {
          method: 'POST',
          body: JSON.stringify({ email: emailToUse, code, activateUser: true })
        })

        if (!response.ok) {
          const message = (payload && payload.detail) || getMessage('otpError', 'Verification failed. Try again.')
          showMessage(otpMessage, message)
          return
        }

        closeModal(otpModal)
        openModal(registerSuccessModal)
        registerEmail = ''
      } catch (error) {
        showMessage(otpMessage, getMessage('otpError', 'Verification failed. Try again.'))
      }
    })

    otpModal.querySelector('[data-action="otp-resend"]')?.addEventListener('click', async (event) => {
      const button = event.currentTarget
      if (button?.dataset?.resendBusy === 'true' || button?.disabled) {
        return
      }
      const params = new URLSearchParams(window.location.search || '')
      const isDisabled = params.get('error') === 'disabled'
      const emailToUse = isDisabled ? (otpEmail || '') : (registerEmail || '')
      if (!emailToUse) {
        showMessage(otpMessage, getMessage('otpError', 'Registration session expired.'))
        return
      }
      markResendPending(button)
      clearMessage(otpMessage)
      try {
        const { response, payload } = await fetchJson('/api/auth/send-code', {
          method: 'POST',
          body: JSON.stringify({ email: emailToUse })
        })
        if (!response.ok) {
          if (response.status === 429) {
            const waitSeconds = resolveCooldownSeconds(payload, response.headers.get('Retry-After'))
            showMessage(otpMessage, getMessage('otpResendWait', 'Please wait {0} seconds before requesting again.').replace('{0}', waitSeconds))
            if (waitSeconds > 0) {
              startCooldown(button, waitSeconds)
            } else {
              resetResendButton(button)
            }
            return
          }
          const message = (payload && payload.detail) || getMessage('otpError', 'Could not resend the code. Try later.')
          showMessage(otpMessage, message)
          resetResendButton(button)
          return
        }
        applyCooldownFromPayload(button, payload, response.headers.get('Retry-After'))
      } catch (error) {
        showMessage(otpMessage, getMessage('otpError', 'Could not resend the code. Try later.'))
        resetResendButton(button)
      }
    })
  }

  if (forgotEmailModal) {
    forgotEmailModal.querySelector('[data-action="forgot-send"]')?.addEventListener('click', async () => {
      clearMessage(forgotEmailMessage)
      const email = forgotEmailInput?.value?.trim() || ''
      if (!email) {
        showMessage(forgotEmailMessage, getMessage('forgotEmailError', 'Enter a valid e-mail.'))
        return
      }
      if (!isEmailValid(email)) {
        showMessage(forgotEmailMessage, getMessage('forgotEmailError', 'Enter a valid e-mail.'))
        return
      }
      forgotEmailInput && (forgotEmailInput.value = email)

      let codeModalOpened = false
      if (forgotCodeModal) {
        forgotEmail = email
        if (forgotCodeDescription) {
          const template = forgotCodeDescription.dataset.template || forgotCodeDescription.textContent || ''
          forgotCodeDescription.textContent = format(template, email)
        }
        if (forgotCodeInput) {
          forgotCodeInput.value = ''
        }
        clearMessage(forgotCodeMessage)
        if (forgotResendButton) {
          markResendPending(forgotResendButton)
        }
        openModal(forgotCodeModal)
        codeModalOpened = true
      }

      try {
        const { response, payload } = await fetchJson('/api/auth/send-code', {
          method: 'POST',
          body: JSON.stringify({ email })
        })
        if (!response.ok) {
          if (codeModalOpened) {
            closeModal(forgotCodeModal)
          }
          forgotEmail = ''
          openModal(forgotEmailModal)
          forgotEmailInput && (forgotEmailInput.value = email)
          const message = (payload && payload.detail) || getMessage('genericError', 'Could not send the code.')
          const emailErrorCode = payload?.errors?.email || payload?.errorCode
          if (emailErrorCode === 'EMAIL_INVALID' || emailErrorCode === 'EMAIL_REQUIRED') {
            showMessage(forgotEmailMessage, getMessage('forgotEmailError', 'Enter a valid e-mail.'))
            return
          }
          if (message && message.toLowerCase().includes('invalid request content')) {
            showMessage(forgotEmailMessage, getMessage('forgotEmailError', 'Enter a valid e-mail.'))
            return
          }
          showMessage(forgotEmailMessage, message)
          return
        }

        if (!codeModalOpened && forgotCodeModal) {
          forgotEmail = email
          if (forgotCodeDescription) {
            const template = forgotCodeDescription.dataset.template || forgotCodeDescription.textContent || ''
            forgotCodeDescription.textContent = format(template, email)
          }
          if (forgotCodeInput) {
            forgotCodeInput.value = ''
          }
          clearMessage(forgotCodeMessage)
          if (forgotResendButton) {
            markResendPending(forgotResendButton)
          }
          openModal(forgotCodeModal)
        }
      } catch (error) {
        if (codeModalOpened) {
          closeModal(forgotCodeModal)
        }
        forgotEmail = ''
        openModal(forgotEmailModal)
        forgotEmailInput && (forgotEmailInput.value = email)
        showMessage(forgotEmailMessage, getMessage('genericError', 'Could not send the code.'))
      }
    })
  }

  if (forgotCodeModal) {
    forgotCodeInput?.addEventListener('input', () => clearMessage(forgotCodeMessage))

    forgotCodeModal.querySelector('[data-action="forgot-verify"]')?.addEventListener('click', async () => {
      clearMessage(forgotCodeMessage)
      const code = forgotCodeInput?.value?.trim() || ''
      if (!code || !/^\d{8}$/.test(code)) {
        showMessage(forgotCodeMessage, getMessage('forgotCodeError', 'Enter a valid code.'))
        return
      }
      if (!forgotEmail) {
        showMessage(forgotCodeMessage, getMessage('forgotCodeError', 'Session expired. Start over.'))
        return
      }

      try {
        const { response, payload } = await fetchJson('/api/auth/verify-code', {
          method: 'POST',
          body: JSON.stringify({ email: forgotEmail, code, activateUser: false })
        })
        if (!response.ok) {
          const errors = payload?.errors || {}
          const codeError = errors.code || payload?.errorCode
          if (codeError === 'VERIFICATION_CODE_INVALID' || codeError === 'VERIFICATION_CODE_EXPIRED') {
            showMessage(forgotCodeMessage, getMessage('forgotCodeError', 'Verification failed.'))
            return
          }
          const message = (payload && payload.detail) || getMessage('forgotCodeError', 'Verification failed.')
          showMessage(forgotCodeMessage, message)
          return
        }
        const token = payload && payload.resetToken
        if (!token) {
          showMessage(forgotCodeMessage, getMessage('forgotCodeError', 'Verification failed.'))
          return
        }
        resetToken = token
        forgotPasswordInput && (forgotPasswordInput.value = '')
        forgotConfirmInput && (forgotConfirmInput.value = '')
        clearMessage(forgotResetMessage)
        openModal(forgotResetModal)
      } catch (error) {
        showMessage(forgotCodeMessage, getMessage('forgotCodeError', 'Verification failed.'))
      }
    })

    forgotCodeModal.querySelector('[data-action="forgot-resend"]')?.addEventListener('click', async (event) => {
      const button = event.currentTarget
      if (button?.dataset?.resendBusy === 'true' || button?.disabled) {
        return
      }
      if (!forgotEmail) {
        showMessage(forgotCodeMessage, getMessage('forgotCodeError', 'Session expired. Start over.'))
        return
      }
      markResendPending(button)
      clearMessage(forgotCodeMessage)
      try {
        const { response, payload } = await fetchJson('/api/auth/send-code', {
          method: 'POST',
          body: JSON.stringify({ email: forgotEmail })
        })
        if (!response.ok) {
          if (response.status === 429) {
            const waitSeconds = resolveCooldownSeconds(payload, response.headers.get('Retry-After'))
            showMessage(forgotCodeMessage, getMessage('otpResendWait', 'Please wait {0} seconds before requesting again.').replace('{0}', waitSeconds))
            if (waitSeconds > 0) {
              startCooldown(button, waitSeconds)
            } else {
              resetResendButton(button)
            }
            return
          }
          const message = (payload && payload.detail) || getMessage('genericError', 'Could not resend the code.')
          showMessage(forgotCodeMessage, message)
          resetResendButton(button)
          return
        }
        applyCooldownFromPayload(button, payload, response.headers.get('Retry-After'))
      } catch (error) {
        showMessage(forgotCodeMessage, getMessage('genericError', 'Could not resend the code.'))
        resetResendButton(button)
      }
    })
  }

  if (forgotResetModal) {
    forgotResetModal.querySelector('[data-action="forgot-complete"]')?.addEventListener('click', async () => {
      clearMessage(forgotResetMessage)
      const password = forgotPasswordInput?.value || ''
      const confirm = forgotConfirmInput?.value || ''

      if (!password || !confirm) {
        showMessage(forgotResetMessage, getMessage('forgotPasswordError', 'Fill in all fields.'))
        return
      }

      if (password !== confirm) {
        showMessage(forgotResetMessage, getMessage('forgotPasswordMismatch', 'Passwords do not match.'))
        return
      }

      if (!isPasswordStrong(password)) {
        showMessage(forgotResetMessage, getMessage('forgotPasswordWeak', 'Password does not meet requirements.'))
        return
      }

      if (!resetToken) {
        showMessage(forgotResetMessage, getMessage('forgotPasswordError', 'Verification expired. Request a new code.'))
        return
      }

      try {
        const { response, payload } = await fetchJson('/api/auth/reset-password', {
          method: 'POST',
          body: JSON.stringify({ email: forgotEmail, password, confirmPassword: confirm, resetToken })
        })
        if (!response.ok) {
          const errors = payload?.errors || {}
          const errorCode = payload?.errorCode
          if (errors.password === 'PASSWORD_WEAK' || errors.password === 'PASSWORD_REQUIRED' || errors.confirmPassword === 'PASSWORD_WEAK' || errors.confirmPassword === 'PASSWORD_REQUIRED' || errorCode === 'PASSWORD_WEAK' || errorCode === 'PASSWORD_REQUIRED') {
            showMessage(forgotResetMessage, getMessage('forgotPasswordWeak', 'Password does not meet requirements.'))
            return
          }
          if (errors.confirmPassword === 'PASSWORDS_DO_NOT_MATCH' || errorCode === 'PASSWORDS_DO_NOT_MATCH') {
            showMessage(forgotResetMessage, getMessage('forgotPasswordMismatch', 'Passwords do not match.'))
            return
          }
          const message = (payload && payload.detail) || getMessage('genericError', 'Could not reset password.')
          showMessage(forgotResetMessage, message)
          return
        }

        resetToken = ''
        closeModal(forgotResetModal)
        openModal(forgotSuccessModal)
      } catch (error) {
        showMessage(forgotResetMessage, getMessage('genericError', 'Could not reset password.'))
      }
    })
  }

  document.addEventListener('keydown', (event) => {
    if (event.key === 'Escape' && activeModal) {
      closeModal(activeModal)
    }
  })

  // Clear signin error only when user edits email or password
  const usernameInput = signinForm?.querySelector('input[name="username"]')
  const passwordInput = signinForm?.querySelector('input[name="password"]')
  usernameInput?.addEventListener('input', () => clearMessage(signinError))
  passwordInput?.addEventListener('input', () => clearMessage(signinError))

  setMode('signin')

  // Open OTP modal automatically after disabled sign-in
  try {
    const params = new URLSearchParams(window.location.search || '')
    const loginError = params.get('error')
    if (loginError === 'disabled' && otpModal) {
      // Try to prefill e-mail from last attempted username rendered by server
      const lastUsernameInput = signinForm?.querySelector('input[name="username"]')
      const usernameVal = (lastUsernameInput?.value || '')
      // For disabled flow prefer email captured from registration if present, otherwise username
      otpEmail = registerEmail || usernameVal

      if (otpDescription) {
        const disabledText = getMessage('otpDisabled', 'Account not verified. We sent a new code to your email.')
        const displayEmail = (otpEmail || registerEmail || signinForm?.querySelector('input[name="username"]')?.value || 'your e-mail')
        otpDescription.textContent = format(disabledText, displayEmail)
      }
      if (otpInput) {
        otpInput.value = ''
      }
      clearMessage(otpMessage)
      openModal(otpModal)
      if (otpResendButton) {
        markResendPending(otpResendButton)
      }

      // Ensure there is a valid code: send new only if absent/expired (fire-and-forget)
      const emailToUse = otpEmail || signinForm?.querySelector('input[name="username"]')?.value || ''
      if (emailToUse) {
        fetchJson('/api/auth/send-code', {
          method: 'POST',
          body: JSON.stringify({ email: emailToUse })
        })
          .then(({ response: ensureResponse, payload: ensurePayload }) => {
            if (!ensureResponse.ok) {
              if (ensureResponse.status === 429 && otpResendButton) {
                applyCooldownFromPayload(otpResendButton, ensurePayload, ensureResponse.headers.get('Retry-After'))
              }
              return
            }
            if (otpResendButton) {
              applyCooldownFromPayload(otpResendButton, ensurePayload, ensureResponse.headers.get('Retry-After'))
            }
          })
          .catch(() => {})
      }
    }
  } catch (e) {
    // no-op
  }
})()


