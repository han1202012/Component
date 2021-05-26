package kim.hsl.library1

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import kim.hsl.router_annotation.Route

@Route(path = "/library1/MainActivity2")
class MainActivity2 : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
    }
}