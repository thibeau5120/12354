// -------------- server.js --------------
require('dotenv').config();
const express = require('express');
const bodyParser = require('body-parser');
const cors = require('cors');
const mongoose = require('mongoose');

const app = express();
const PORT = process.env.PORT || 3000;

// Middleware
app.use(cors());
app.use(bodyParser.json());
app.use(express.static('public'));

// Connexion à la base de données
mongoose.connect(process.env.MONGO_URI, { useNewUrlParser: true, useUnifiedTopology: true })
    .then(() => console.log('Connexion à MongoDB réussie'))
    .catch(err => console.error('Erreur de connexion à MongoDB', err));

// Routes de l'application
const authRoutes = require('./backend/routes/authRoutes');
app.use('/api/auth', authRoutes);

// Lancement du serveur
app.listen(PORT, () => {
    console.log(`Serveur en cours d'exécution sur http://localhost:${PORT}`);
});


// -------------- .env --------------
PORT=3000
MONGO_URI=mongodb://127.0.0.1:27017/2fa_db
JWT_SECRET=super_secret_key


// -------------- backend/models/User.js --------------
const mongoose = require('mongoose');
const bcrypt = require('bcrypt');

const userSchema = new mongoose.Schema({
    email: { type: String, required: true, unique: true },
    password: { type: String, required: true },
    bluetoothMac: { type: String, required: false },  // Adresse MAC Bluetooth de l'utilisateur
    voiceSignature: { type: String, required: false } // Empreinte vocale (hachée)
});

// Hachage du mot de passe avant l'enregistrement
userSchema.pre('save', async function(next) {
    if (!this.isModified('password')) return next();
    const salt = await bcrypt.genSalt(10);
    this.password = await bcrypt.hash(this.password, salt);
    next();
});

module.exports = mongoose.model('User', userSchema);


// -------------- backend/routes/authRoutes.js --------------
const express = require('express');
const router = express.Router();
const User = require('../models/User');
const bcrypt = require('bcrypt');
const jwt = require('jsonwebtoken');

// Route d'inscription
router.post('/signup', async (req, res) => {
    const { email, password } = req.body;
    try {
        const newUser = new User({ email, password });
        await newUser.save();
        res.status(201).json({ message: 'Inscription réussie' });
    } catch (error) {
        res.status(400).json({ error: "Erreur d'inscription" });
    }
});

// Route de connexion
router.post('/login', async (req, res) => {
    const { email, password } = req.body;
    const user = await User.findOne({ email });
    if (!user) return res.status(404).json({ error: 'Utilisateur introuvable' });

    const isMatch = await bcrypt.compare(password, user.password);
    if (!isMatch) return res.status(401).json({ error: 'Mot de passe incorrect' });

    const token = jwt.sign({ id: user._id }, process.env.JWT_SECRET, { expiresIn: '1h' });
    res.status(200).json({ token });
});

module.exports = router;


// -------------- public/index.html --------------
<!DOCTYPE html>
<html lang="fr">
<head>
    <meta charset="UTF-8">
    <title>Authentification 2FA</title>
    <link rel="stylesheet" href="styles.css">
</head>
<body>
    <h1>Connexion 2FA</h1>

    <div id="login-form">
        <input type="email" id="email" placeholder="Email">
        <input type="password" id="password" placeholder="Mot de passe">
        <button id="login-btn">Se connecter</button>
    </div>

    <script src="script.js"></script>
</body>
</html>


// -------------- public/styles.css --------------
body {
    font-family: Arial, sans-serif;
    background-color: #f5f5f5;
    text-align: center;
}

#login-form {
    margin-top: 50px;
    padding: 20px;
    background: white;
    border-radius: 8px;
    display: inline-block;
}

input {
    display: block;
    width: 100%;
    margin: 10px 0;
    padding: 10px;
}


// -------------- public/script.js --------------
document.getElementById('login-btn').addEventListener('click', async () => {
    const email = document.getElementById('email').value;
    const password = document.getElementById('password').value;

    const response = await fetch('/api/auth/login', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ email, password })
    });

    const result = await response.json();
    if (result.token) {
        alert('Connexion réussie !');
        localStorage.setItem('token', result.token);
    } else {
        alert(result.error);
    }
});
