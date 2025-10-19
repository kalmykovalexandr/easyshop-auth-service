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

  const i18nStore = document.querySelector('[data-i18n-store]')
  const messages = i18nStore ? { ...i18nStore.dataset } : {}

  const PASSWORD_REGEX = /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[@$!%*?&])[A-Za-z\d@$!%*?&]{8,}$/

  const otpModal = document.querySelector('[data-modal="otp"]')
  const otpInput = otpModal?.querySelector('[data-otp-input]')
  const otpMessage = otpModal?.querySelector('[data-otp-message]')
  const otpDescription = otpModal?.querySelector('[data-otp-description]')

  const registerSuccessModal = document.querySelector('[data-modal="register-success"]')
  const forgotEmailModal = document.querySelector('[data-modal="forgot-email"]')
  const forgotEmailInput = forgotEmailModal?.querySelector('[data-forgot-email-input]')
  const forgotEmailMessage = forgotEmailModal?.querySelector('[data-forgot-email-message]')

  const forgotCodeModal = document.querySelector('[data-modal="forgot-code"]')
  const forgotCodeInput = forgotCodeModal?.querySelector('[data-forgot-code-input]')
  const forgotCodeMessage = forgotCodeModal?.querySelector('[data-forgot-code-message]')
  const forgotCodeDescription = forgotCodeModal?.querySelector('[data-forgot-code-description]')

  const forgotResetModal = document.querySelector('[data-modal="forgot-reset"]')
  const forgotPasswordInput = forgotResetModal?.querySelector('[data-forgot-password-input]')
  const forgotConfirmInput = forgotResetModal?.querySelector('[data-forgot-confirm-input]')
  const forgotResetMessage = forgotResetModal?.querySelector('[data-forgot-reset-message]')

  const forgotSuccessModal = document.querySelector('[data-modal="forgot-success"]')

  let activeModal = null
  let registerEmail = ''
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
    setMode('signin')
  })

  forgotSuccessModal?.querySelector('[data-action="close-modal"]')?.addEventListener('click', () => {
    setMode('signin')
  })

  document.querySelectorAll('[data-action="go-signin"]').forEach((button) => {
    button.addEventListener('click', () => {
      closeAllModals()
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

  const passwordToggles = Array.from(root.querySelectorAll('[data-toggle-password]'))

  passwordToggles.forEach((button) => {
    const input = button?.parentElement?.querySelector('input')
    if (!input) {
      button.classList.add('auth-field__toggle--hidden')
      return
    }

    const updateToggleVisibility = () => {
      const hasValue = Boolean(input.value && input.value.length > 0)
      button.classList.toggle('auth-field__toggle--hidden', !hasValue)
    }

    input.addEventListener('input', updateToggleVisibility)
    input.addEventListener('change', updateToggleVisibility)
    updateToggleVisibility()

    button.addEventListener('click', (event) => {
      event.preventDefault()
      togglePasswordVisibility(button)
      input.focus()
    })
  })

  function showMessage(container, text, type = 'error') {
    if (!container) return
    container.textContent = text
    container.hidden = false
    container.setAttribute('aria-hidden', 'false')
    container.classList.remove('auth-message--error', 'auth-message--success')
    container.classList.add(type === 'success' ? 'auth-message--success' : 'auth-message--error')
  }

  function clearMessage(container) {
    if (!container) return
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

  function startCooldown(button, seconds) {
    if (!button) return
    let remaining = Math.max(Number(seconds) || 0, 0)
    button.disabled = true
    const original = button.dataset.originalText || button.textContent
    button.dataset.originalText = original

    const update = () => {
      if (remaining <= 0) {
        button.disabled = false
        button.textContent = original
        if (resendTimer) {
          window.clearInterval(resendTimer)
          resendTimer = null
        }
        return
      }
      button.textContent = `${original} (${remaining}s)`
      remaining -= 1
    }
    update()
    resendTimer = window.setInterval(update, 1000)
  }

  if (registerForm) {
    registerForm.addEventListener('submit', async (event) => {
      event.preventDefault()
      clearMessage(registerError)

      const emailInput = registerForm.querySelector('input[name="email"]')
      const passwordInput = registerForm.querySelector('input[name="password"]')
      const confirmInput = registerForm.querySelector('input[name="confirmPassword"]')

      const email = emailInput?.value?.trim() || ''
      const password = passwordInput?.value || ''
      const confirmPassword = confirmInput?.value || ''

      if (!email) {
        showMessage(registerError, registerForm.dataset.errorEmail || 'Enter a valid e-mail.')
        emailInput?.focus()
        return
      }

      if (!password) {
        showMessage(registerError, registerForm.dataset.errorPassword || 'Enter a password.')
        passwordInput?.focus()
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

      let otpModalOpened = false
      if (otpModal) {
        registerEmail = email
        if (otpDescription) {
          const template = otpDescription.dataset.template || otpDescription.textContent || ''
          otpDescription.textContent = format(template, registerEmail)
        }
        if (otpInput) {
          otpInput.value = ''
        }
        clearMessage(otpMessage)
        openModal(otpModal)
        otpModalOpened = true
      }

      try {
        const { response, payload } = await fetchJson('/api/auth/register', {
          method: 'POST',
          body: JSON.stringify({ email, password, confirmPassword })
        })

        if (!response.ok) {
          if (otpModalOpened) {
            closeModal(otpModal)
            registerEmail = ''
          }
          const message = (payload && payload.detail) || registerForm.dataset.errorGeneric || getMessage('genericError', 'Could not create the account.')
          showMessage(registerError, message)
          return
        }

        if (!otpModalOpened && otpModal) {
          registerEmail = email
          if (otpDescription) {
            const template = otpDescription.dataset.template || otpDescription.textContent || ''
            otpDescription.textContent = format(template, registerEmail)
          }
          if (otpInput) {
            otpInput.value = ''
          }
          clearMessage(otpMessage)
          openModal(otpModal)
        }
      } catch (error) {
        if (otpModalOpened) {
          closeModal(otpModal)
          registerEmail = ''
        }
        showMessage(registerError, registerForm.dataset.errorGeneric || getMessage('genericError', 'Something went wrong. Try again later.'))
      }
    })
  }

  if (otpModal) {
    otpModal.querySelector('[data-action="otp-submit"]')?.addEventListener('click', async () => {
      const code = otpInput?.value?.trim() || ''
      if (!code || !/^\d{8}$/.test(code)) {
        showMessage(otpMessage, getMessage('otpError', 'Enter a valid code.'))
        return
      }

      if (!registerEmail) {
        showMessage(otpMessage, getMessage('otpError', 'Registration session expired.'))
        return
      }

      clearMessage(otpMessage)

      try {
        const { response, payload } = await fetchJson('/api/auth/verify-code', {
          method: 'POST',
          body: JSON.stringify({ email: registerEmail, code, activateUser: true })
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
      if (!registerEmail) {
        showMessage(otpMessage, getMessage('otpError', 'Registration session expired.'))
        return
      }
      clearMessage(otpMessage)
      try {
        const { response, payload } = await fetchJson('/api/auth/send-verification-code', {
          method: 'POST',
          body: JSON.stringify({ email: registerEmail })
        })
        if (!response.ok) {
          const retryAfter = response.headers.get('Retry-After')
          if (response.status === 429 && retryAfter) {
            showMessage(otpMessage, getMessage('otpResendWait', 'Please wait {0} seconds before requesting again.').replace('{0}', retryAfter))
            startCooldown(button, Number(retryAfter))
            return
          }
          const message = (payload && payload.detail) || getMessage('otpError', 'Could not resend the code. Try later.')
          showMessage(otpMessage, message)
          return
        }
        showMessage(otpMessage, getMessage('otpResendSuccess', 'We sent a new code.'), 'success')
        startCooldown(button, Number(response.headers.get('Retry-After')))
      } catch (error) {
        showMessage(otpMessage, getMessage('otpError', 'Could not resend the code. Try later.'))
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
        openModal(forgotCodeModal)
        codeModalOpened = true
      }

      try {
        const { response, payload } = await fetchJson('/api/auth/send-verification-code', {
          method: 'POST',
          body: JSON.stringify({ email })
        })
        if (!response.ok) {
          if (codeModalOpened) {
            closeModal(forgotCodeModal)
          }
          forgotEmail = ''
          const retryAfter = response.headers.get('Retry-After')
          openModal(forgotEmailModal)
          forgotEmailInput && (forgotEmailInput.value = email)
          if (response.status === 429 && retryAfter) {
            showMessage(forgotEmailMessage, getMessage('otpResendWait', 'Please wait {0} seconds before requesting again.').replace('{0}', retryAfter))
            return
          }
          const message = (payload && payload.detail) || getMessage('genericError', 'Could not send the code.')
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
      if (!forgotEmail) {
        showMessage(forgotCodeMessage, getMessage('forgotCodeError', 'Session expired. Start over.'))
        return
      }
      clearMessage(forgotCodeMessage)
      try {
        const { response, payload } = await fetchJson('/api/auth/send-verification-code', {
          method: 'POST',
          body: JSON.stringify({ email: forgotEmail })
        })
        if (!response.ok) {
          const retryAfter = response.headers.get('Retry-After')
          if (response.status === 429 && retryAfter) {
            showMessage(forgotCodeMessage, getMessage('otpResendWait', 'Please wait {0} seconds before requesting again.').replace('{0}', retryAfter))
            startCooldown(button, Number(retryAfter))
            return
          }
          const message = (payload && payload.detail) || getMessage('genericError', 'Could not resend the code.')
          showMessage(forgotCodeMessage, message)
          return
        }
        showMessage(forgotCodeMessage, getMessage('otpResendSuccess', 'We sent a new code.'), 'success')
        startCooldown(button, Number(response.headers.get('Retry-After')))
      } catch (error) {
        showMessage(forgotCodeMessage, getMessage('genericError', 'Could not resend the code.'))
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

  // Helper to clear signin client errors on input
  signinForm?.addEventListener('input', () => {
    clearMessage(signinError)
  })

  setMode('signin')
})()

