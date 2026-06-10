package ui.image

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import coil.load
import lab3.chat.R
import lab3.chat.databinding.FragmentImageBinding

class ImageFragment : Fragment(R.layout.fragment_image) {
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val binding = FragmentImageBinding.bind(view)
        val path = arguments?.getString("imagePath").orEmpty()
        val imageSource = if (
            path.startsWith("content://") ||
            path.startsWith("file://") ||
            path.startsWith("http://") ||
            path.startsWith("https://")
        ) {
            path
        } else {
            "https://faerytea.name/img/$path"
        }
        binding.fullImageView.load(imageSource)
        binding.closeImageButton.setOnClickListener {
            findNavController().navigateUp()
        }
    }
}


