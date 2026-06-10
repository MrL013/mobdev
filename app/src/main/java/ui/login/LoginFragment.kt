package ui.login

import android.app.AlertDialog
import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import kotlinx.coroutines.launch
import lab3.chat.R
import app.appContainer
import lab3.chat.databinding.FragmentLoginBinding

class LoginFragment : Fragment(R.layout.fragment_login) {
    private var binding: FragmentLoginBinding? = null

    private val viewModel: LoginViewModel by viewModels {
        val container = appContainer()
        LoginViewModelFactory(
            container.repository,
            !container.session.login.isNullOrBlank() && !container.session.password.isNullOrBlank()
        )
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val b = FragmentLoginBinding.bind(view)
        binding = b

        b.loginButton.setOnClickListener {
            viewModel.login(b.loginEditText.text.toString().trim(), b.passwordEditText.text.toString())
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.uiState.collect { state ->
                b.loginButton.isEnabled = !state.loading
                if (state.success) {
                    findNavController().navigate(R.id.action_loginFragment_to_chatHostFragment)
                } else if (state.errorRes != null) {
                    AlertDialog.Builder(requireContext())
                        .setMessage(state.errorRes)
                        .setPositiveButton(android.R.string.ok, null)
                        .show()
                    viewModel.consumeError()
                }
            }
        }
    }

    override fun onDestroyView() {
        binding = null
        super.onDestroyView()
    }
}


