package ru.haritonova.saturday10.presentation.goods.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient.Builder
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import ru.haritonova.saturday10.App
import ru.haritonova.saturday10.data.api.ApiExample
import ru.haritonova.saturday10.data.models.UserModel
import ru.haritonova.saturday10.presentation.goods.contract.GoodsEffect
import ru.haritonova.saturday10.presentation.goods.contract.GoodsEvent
import ru.haritonova.saturday10.presentation.goods.contract.GoodsEvent.AddButtonClicked
import ru.haritonova.saturday10.presentation.goods.contract.GoodsEvent.OnUserItemClick
import ru.haritonova.saturday10.presentation.goods.contract.GoodsEvent.UpdateGoodsTextField
import ru.haritonova.saturday10.presentation.goods.contract.GoodsEvent.UpdateGoodsUrlField
import ru.haritonova.saturday10.presentation.goods.contract.GoodsState


class GoodsViewModel : ViewModel() {

  val state = MutableStateFlow(GoodsState())

  private val _effect = Channel<GoodsEffect>()
  val effect = _effect.receiveAsFlow()

  private fun getClient(): ApiExample {
    val httpClient = Builder()
    val logging = HttpLoggingInterceptor()
    logging.setLevel(HttpLoggingInterceptor.Level.BODY)
    httpClient.addInterceptor(logging)
    val retrofit = Retrofit.Builder()
      .baseUrl("https://api.github.com/")
      .addConverterFactory(GsonConverterFactory.create())
      .client(httpClient.build())
      .build()

    return retrofit.create(ApiExample::class.java)
  }

  fun handleEvent(event: GoodsEvent) {
    when (event) {
      is UpdateGoodsTextField -> {
        state.value = state.value.copy(goodsName = event.text)
      }

      is OnUserItemClick -> {
        viewModelScope.launch {
          _effect.send(GoodsEffect.OpenDetails(event.user))
        }
      }

      AddButtonClicked -> {
        App.getDatabase()?.clearAllTables()
        val client = getClient()

        viewModelScope.launch {
          try {
            App.getDatabase()?.userDao()?.let { dao ->
              val users = client.getUsers()

              state.value = state.value.copy(
                users = users.map {
                  UserModel(
                    id = it.id,
                    login = it.login,
                    imageUrl = it.imageUrl,
                    type = it.type,
                    user_view_type = it.user_view_type
                  )
                },
                goodsName = ""
              )
            }
          } catch (e: Exception) {
            e.printStackTrace()
          }
        }
      }

      is UpdateGoodsUrlField -> {
        state.value = state.value.copy(goodsUrl = event.url)
      }
    }
  }

  fun isNumeric(toCheck: String): Boolean {
    return toCheck.all { char -> char.isDigit() }
  }
}