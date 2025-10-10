(function () {
  const root = document.querySelector('[data-auth-root]')
  if (!root) {
    return
  }

  const card = root.querySelector('.auth-card')
  const loginSection = root.querySelector('[data-view="signin"]')
  const registerSection = root.querySelector('[data-view="register"]')
  const registerForm = root.querySelector('#register-form')

  const passwordToggleButtons = Array.from(root.querySelectorAll('[data-toggle-password]'))

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

  const emailField = registerForm.querySelector('input[name="email"]')
  const passwordField = registerForm.querySelector('input[name="password"]')
  const confirmField = registerForm.querySelector('input[name="confirmPassword"]')
  const feedback = registerForm.querySelector('[data-feedback]')
  const submitButton = registerForm.querySelector('button[type="submit"]')

  const messages = {
    success: registerForm.dataset.successMessage || 'Account created. You can sign in now.',
    errorGeneric: registerForm.dataset.errorGeneric || 'We could not create the account. Please try again later.',
    errorNetwork: registerForm.dataset.errorNetwork || 'Network error. Please try again.',
    errorMismatch: registerForm.dataset.errorMismatch || 'Passwords do not match.',
    errorEmpty: registerForm.dataset.errorEmpty || 'Fill in all fields.',
    errorEmail: registerForm.dataset.errorEmail || 'Enter a valid e-mail address.',
    errorPassword: registerForm.dataset.errorPassword || 'Password must be at least 8 characters and include upper and lower case letters, a number, and one of @$!%*?&.'
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

      registerForm.reset()
      resetValidationState()
      resetPasswordToggles()
      setFeedback('success', messages.success)
    } catch (error) {
      setFeedback('error', messages.errorNetwork)
    } finally {
      toggleLoading(false)
    }
  }

  registerForm.addEventListener('submit', handleSubmit)
})()

