function sleep(ms) {
	return new Promise(resolve => setTimeout(resolve, ms));
}

function getRandomInt() {
  return Math.floor(1000 + Math.random() * 9000);
}

module.exports = {
 sleep, getRandomInt
}