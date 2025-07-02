function sleep(ms) {
	return new Promise(resolve => setTimeout(resolve, ms));
}

function getRandomInt() {
  return Math.floor(10000 + Math.random() * 90000);
}

module.exports = {
 sleep, getRandomInt
}