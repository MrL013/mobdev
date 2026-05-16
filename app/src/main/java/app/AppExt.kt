package app

import androidx.fragment.app.Fragment

fun Fragment.appContainer() = (requireActivity().application as ChatApp).container