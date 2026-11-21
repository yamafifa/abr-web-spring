// 画面の JS の入り口。Stimulus のコントローラーを登録する
import { Application } from '@hotwired/stimulus';
import './app.css';

const app = Application.start();
// controllers/ の xxx_controller.js を、"xxx"（_ は -）という名前で登録する
for (const [path, module] of Object.entries(import.meta.glob('./controllers/*_controller.js', { eager: true }))) {
    const name = path.match(/([^/]+)_controller\.js$/)[1].replaceAll('_', '-');
    app.register(name, module.default);
}
