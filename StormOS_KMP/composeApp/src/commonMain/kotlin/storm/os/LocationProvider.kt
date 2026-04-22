package storm.os

// Adiciona o parâmetro Any para representar o Context no Android
expect suspend fun getCurrentLocation(): Pair<Double, Double>?