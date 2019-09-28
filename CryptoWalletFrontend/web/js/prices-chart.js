const currency = {}

/**
 * @param {number} value
 */
currency.format = function (value) {
    if (typeof value == 'string')
        value = parseFloat(value)

    if (value < 1000)
        return "$ " + value.toFixed(2)

    let formated = value.toFixed(2)
    let length = formated.length - 3
    let pos = length - 3

    while (pos > 0) {
        const first = formated.slice(0, pos)
        const second = formated.slice(pos)

        formated = `${first},${second}`
        pos -= 3
    }

    return "$ " + formated
}

const PriceChart = {}

/**
 * Crea una gráfica de precio en el objeto canvas especificado
 * @param {HTMLCanvasElement} context
 * @param {Array<number>} data
 * @param {Array<string>} labels
 * @param {string} pair
 * @param {string} colorLine
 * @returns {void}
 */
PriceChart.create = (context, data, labels, pair, colorLine) => {
    new Chart(context, {
        type: "line",
        data: {
            labels: labels,
            datasets: [{
                label: pair,
                data: data,
                fill: false,
                borderColor: colorLine,
                lineTension: 0.1,
                pointRadius: 1,
                backgroundColor: colorLine
            }]
        },
        options: {
            tooltips: {
                callbacks: {
                    label: (item) => item.value = currency.format(item.value)
                }
            },
            legend: {
                display: false
            },
            scales: {
                xAxes: [{
                    gridLines: {
                        lineWidth: 0
                    },
                    display: false
                }],
                yAxes: [{
                    ticks: {
                        callback: currency.format
                    }
                }]
            }
        }
    })
}

/**
 * Obtiene la data utilizada para rellenar las graficas de precio.
 * @param {string} method
 * @param {string} url
 * @returns {Promise<{labels: Array<string>, data: Array<number>}>}
 */
PriceChart.getDataSet = (method, url) => new Promise(resolve => {
    let ajax = new XMLHttpRequest()
    ajax.onload = (res) => {
        const response = JSON.parse(res.target.responseText)
        const limit = 200
        const data = response.slice(500 - limit, 500).map(kline => parseFloat(kline[4]))
        const startTime = new Date(response[0][6])
        const labels = Array(500).fill(0)
            .map((ign, idx) => new Date(startTime.getTime() + idx * 3600000 * 24).toLocaleString())
            .slice(500 - data.length, 500)

        labels[labels.length - 1] = "Ahora"

        resolve({
            labels,
            data
        })
    }
    ajax.open(method, url)
    ajax.send()
})

PriceChart.getAssets = async () => [{
        src: '../images/bitcoin.png',
        name: 'Bitcoin',
        url: '../js/btcusdt.json',
        pair: 'BTCUSDT',
        color: 'rgb(247,147,26)'
    },
    {
        src: '../images/ethereum.png',
        name: 'Ethereum',
        url: '../js/ethusdt.json',
        pair: 'ETHUSDT',
        color: 'rgb(173,205,254)'
    },
    {
        src: '../images/litecoin.png',
        name: 'Litecoin',
        url: '../js/ltcusdt.json',
        pair: 'LTCUSDT',
        color: 'rgb(165,214,167)'
    },
    {
        src: '../images/trx.png',
        name: 'Tron',
        url: '../js/trxusdt.json',
        pair: 'TRXUSDT',
        color: 'rgb(186,53,26)'
    }
]