package app

import Captura.LoocaMonitor

open class Main {
    companion object {
        @JvmStatic fun main(args: Array<String>) {

            var looca = LoocaMonitor()

    looca.configurar()
    looca.criarTabela()


while (true){
    val captura = looca.capturarDados()
    looca.inserirDados(looca.capturarDados())
    println(captura)
    Thread.sleep(500)

}

}}}