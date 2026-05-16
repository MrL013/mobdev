package ui.image

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import coil.load
import lab3.chat.R
import lab3.chat.databinding.FragmentImageBinding

class ImageFragment : Fragment(R.layout.fragment_image) {
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val binding = FragmentImageBinding.bind(view)
        val path = arguments?.getString("imagePath").orEmpty()
        binding.fullImageView.load("https://faerytea.name/img/$path")
    }
}


