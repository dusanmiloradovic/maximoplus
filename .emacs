
(setq url-proxy-services '( ("http" . "localhost:3128")
			    ("https" . "localhost:3128")
			    ))
(require 'package)

;;(add-to-list 'package-archives '("marmalade" . "http://marmalade-repo.org/packages/"))
(add-to-list 'package-archives '("marmalade" . "http://stable.melpa.org/packages/"))

(add-to-list 'package-archives '("melpa" . "http://melpa.milkbox.net/packages/"))
					;(add-to-list 'package-archives '("joseito" . "http://joseito.republika.pl/sunrise-commander/"))
(add-to-list 'package-archives '("orgmode" . "http://orgmode.org/elpa/"))

(defun my-add-path (path-element)
  "Add the specified path element to the Emacs PATH"
  (interactive "DEnter directory to be added to path: ")
  (if (file-directory-p path-element)
      (progn
	(add-to-list 'exec-path (expand-file-name path-element))
	(setenv "PATH"
		(concat (expand-file-name path-element)
			path-separator (getenv "PATH"))))))

(my-add-path "c:\\dusan\\lein2.6")

(my-add-path "c:\\dusan\\DJ dec 3.0\\")
;;(my-add-path "c:\\dusan\\java9\\jdk9\\bin\\")
(my-add-path "c:\\dusan\\jdk1.8\\bin\\")
;;(my-add-path "c:\\dusan\\jdk1.7.0_07\\bin\\")


(my-add-path "c:\\dusan\\node-v7.9.0-win-x64")
(my-add-path "c:\\dusan")
(my-add-path "c:\\dusan\\putty\\")

(my-add-path "c:\\dusan\\PortableGit\\usr\\bin\\")

(setq openwith-associations '(("\\.class\\'" "jad" (file))))

(setq nrepl-popup-stacktraces nil)
(setq nrepl-popup-stacktraces-in-repl t)
(require 'tramp)
(setq tramp-verbose 10)
(setq tramp-default-method "plink")
(setq tramp-debug-buffer t)


(package-initialize)
(load-theme 'alect-light-alt t)

					;(require 'prettier-js)

;;(customize-set-variable 'jdecomp-decompiler-type 'cfr)

(customize-set-variable 'jdecomp-decompiler-type 'cfr)

(customize-set-variable 'jdecomp-decompiler-paths
                        '((cfr . "c:/dusan/Decompiler/cfr_0_87.jar")
                          (fernflower . "c:/dusan/IntelliJ/plugins/java-decompiler/lib/java-decompiler.jar")
                          (procyon . "c:/dusan/Decompiler/procyon-decompiler-0.5.30.jar")))


(customize-set-variable 'jdecomp-decompiler-options
                        '((cfr "--comments false" "--removeboilerplate false")
                          (fernflower "-hes=0" "-hdc=0")))
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;
;; Allow input to be sent to somewhere other than inferior-lisp
;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;; This is a total hack: we're hardcoding the name of the shell buffer
;;(defun shell-send-input (input)
;;  "Send INPUT into the *shell* buffer and leave it visible."
;;  (save-selected-window
;;    (switch-to-buffer-other-window "*shell*")
;;    (goto-char (point-max))
;;    (insert input)
;;    (comint-send-input)))
;;
;;(defun defun-at-point ()
;;  "Return the text of the defun at point."
;;  (apply #'buffer-substring-no-properties
;;         (region-for-defun-at-point)))
;;
;;(defun region-for-defun-at-point ()
;;  "Return the start and end position of defun at point."
;;  (save-excursion
;;    (save-match-data
;;      (end-of-defun)
;;      (let ((end (point)))
;;        (beginning-of-defun)
;;        (list (point) end)))))
;;
;;(defun expression-preceding-point ()
;;  "Return the expression preceding point as a string."
;;  (buffer-substring-no-properties
;;   (save-excursion (backward-sexp) (point))
;;   (point)))
;;
;;(defun shell-eval-last-expression ()
;;  "Send the expression preceding point to the *shell* buffer."
;;  (interactive)
;;  (shell-send-input (expression-preceding-point)))
;;
;;(defun shell-eval-defun ()
;;  "Send the current toplevel expression to the *shell* buffer."
;;  (interactive)
;;  (shell-send-input (defun-at-point)))
;;
;;(add-hook 'clojurescript-mode-hook
;;          '(lambda ()
;;             (define-key clojure-mode-map (kbd "C-c e") 'shell-eval-last-expression)
;;             (define-key clojure-mode-map (kbd "C-c x") 'shell-eval-defun)))
;;

(defun cljs-node-repl ()
  (interactive)
  (run-clojure "lein trampoline run -m clojure.main repl.clj"))

(defun cljs-old-repl ()
  (interactive)
  (run-clojure "lein trampoline cljsbuild repl-listen"))
(custom-set-faces
 ;; custom-set-faces was added by Custom.
 ;; If you edit it by hand, you could mess it up, so be careful.
 ;; Your init file should contain only one such instance.
 ;; If there is more than one, they won't work right.
 )


(setq inhibit-startup-screen t)

(tool-bar-mode -1)
(menu-bar-mode -1)

(jdecomp-mode 1)

;;(custom-set-variables
;; ;; custom-set-variables was added by Custom.
;; ;; If you edit it by hand, you could mess it up, so be careful.
;; ;; Your init file should contain only one such instance.
;; ;; If there is more than one, they won't work right.
;; '(cider-cljs-lein-repl
;;   "(do (require 'weasel.repl.websocket) (cemerick.piggieback/cljs-repl (weasel.repl.websocket/repl-env :ip \"127.0.0.1\" :port 9001)))"))
;;

(custom-set-variables
 ;; custom-set-variables was added by Custom.
 ;; If you edit it by hand, you could mess it up, so be careful.
 ;; Your init file should contain only one such instance.
 ;; If there is more than one, they won't work right.
 '(Linum-format "%7i ")
 '(ansi-color-names-vector
   ["#5f5f5f" "#ff4b4b" "#d7ff5f" "#fce94f" "#729fcf" "#d18aff" "#afd7ff" "#ffffff"])
 '(background-color "#202020")
 '(background-mode dark)
 '(cider-cljs-lein-repl
   "(do (require 'weasel.repl.websocket) (cemerick.piggieback/cljs-repl (weasel.repl.websocket/repl-env :ip \"127.0.0.1\" :port 9001)))")
 '(compilation-message-face (quote default))
 '(cua-global-mark-cursor-color "#2aa198")
 '(cua-normal-cursor-color "#657b83")
 '(cua-overwrite-cursor-color "#b58900")
 '(cua-read-only-cursor-color "#859900")
 '(cursor-color "#cccccc")
 '(custom-safe-themes
   (quote
    ("85d1dbf2fc0e5d30f236712b831fb24faf6052f3114964fdeadede8e1b329832" "7fa9dc3948765d7cf3d7a289e40039c2c64abf0fad5c616453b263b601532493" "ab04c00a7e48ad784b52f34aa6bfa1e80d0c3fcacc50e1189af3651013eb0d58" "73c69e346ec1cb3d1508c2447f6518a6e582851792a8c0e57a22d6b9948071b4" "5e3fc08bcadce4c6785fc49be686a4a82a356db569f55d411258984e952f194a" "7153b82e50b6f7452b4519097f880d968a6eaf6f6ef38cc45a144958e553fbc6" "bf798e9e8ff00d4bf2512597f36e5a135ce48e477ce88a0764cfb5d8104e8163" "a77ced882e25028e994d168a612c763a4feb8c4ab67c5ff48688654d0264370c" "d91ef4e714f05fff2070da7ca452980999f5361209e679ee988e3c432df24347" "9a155066ec746201156bb39f7518c1828a73d67742e11271e4f24b7b178c4710" "ac191d0ff71167c4b07d38eb2480eeba3eead12c0c60a7dce150627722c79e62" "88d556f828e4ec17ac074077ef9dcaa36a59dccbaa6f2de553d6528b4df79cbd" "60a2ebd7effefeb960f61bc4772afd8b1ae4ea48fae4d732864ab9647c92093a" "a2e0816c1a4bea13ac9c7b7c84f22408e1ffe23cfef4c6c75a71e3dafdc9343b" "1177fe4645eb8db34ee151ce45518e47cc4595c3e72c55dc07df03ab353ad132" "88b663861db4767f7881e5ecff9bb46d65161a20e40585c8128e8bed8747dae5" "c1fb68aa00235766461c7e31ecfc759aa2dd905899ae6d95097061faeb72f9ee" "246a51f19b632c27d7071877ea99805d4f8131b0ff7acb8a607d4fd1c101e163" "d677ef584c6dfc0697901a44b885cc18e206f05114c8a3b7fde674fce6180879" "1a093e45e4c3e86fa5ad1f8003660e7cda4d961cd5d377cee3fee2dad2faf19b" "383806d341087214fd44864170161c6bf34a41e866f501d1be51883e08cb674b" "446c73cdfb49f1dab4c322e51ac00a536fb0e3cb7e6809b9f4616e0858012e92" "fe0a47cc3952fede574527a1c28ddf3a1af381fc1fb5843ca60d22e4c841011a" "efb148b9a120f417464713fe6cad47eb708dc45c7f2dbfeea4a7ec329214e63e" default)))
 '(diary-entry-marker (quote font-lock-variable-name-face))
 '(emms-mode-line-icon-image-cache
   (quote
    (image :type xpm :ascent center :data "/* XPM */
static char *note[] = {
/* width height num_colors chars_per_pixel */
\"    10   11        2            1\",
/* colors */
\". c #358d8d\",
\"# c None s None\",
/* pixels */
\"###...####\",
\"###.#...##\",
\"###.###...\",
\"###.#####.\",
\"###.#####.\",
\"#...#####.\",
\"....#####.\",
\"#..######.\",
\"#######...\",
\"######....\",
\"#######..#\" };")))
 '(fci-rule-character-color "#202020")
 '(fci-rule-color "#c7c7c7")
 '(foreground-color "#cccccc")
 '(frame-brackground-mode (quote dark))
 '(fringe-mode 10 nil (fringe))
 '(gnus-logo-colors (quote ("#0d7b72" "#adadad")) t)
 '(gnus-mode-line-image-cache
   (quote
    (image :type xpm :ascent center :data "/* XPM */
static char *gnus-pointer[] = {
/* width height num_colors chars_per_pixel */
\"    18    13        2            1\",
/* colors */
\". c #358d8d\",
\"# c None s None\",
/* pixels */
\"##################\",
\"######..##..######\",
\"#####........#####\",
\"#.##.##..##...####\",
\"#...####.###...##.\",
\"#..###.######.....\",
\"#####.########...#\",
\"###########.######\",
\"####.###.#..######\",
\"######..###.######\",
\"###....####.######\",
\"###..######.######\",
\"###########.######\" };")) t)
 '(highlight-changes-colors (quote ("#d33682" "#6c71c4")))
 '(highlight-symbol-colors
   (--map
    (solarized-color-blend it "#fdf6e3" 0.25)
    (quote
     ("#b58900" "#2aa198" "#dc322f" "#6c71c4" "#859900" "#cb4b16" "#268bd2"))))
 '(highlight-symbol-foreground-color "#586e75")
 '(highlight-tail-colors
   (quote
    (("#eee8d5" . 0)
     ("#B4C342" . 20)
     ("#69CABF" . 30)
     ("#69B7F0" . 50)
     ("#DEB542" . 60)
     ("#F2804F" . 70)
     ("#F771AC" . 85)
     ("#eee8d5" . 100))))
 '(hl-bg-colors
   (quote
    ("#DEB542" "#F2804F" "#FF6E64" "#F771AC" "#9EA0E5" "#69B7F0" "#69CABF" "#B4C342")))
 '(hl-fg-colors
   (quote
    ("#fdf6e3" "#fdf6e3" "#fdf6e3" "#fdf6e3" "#fdf6e3" "#fdf6e3" "#fdf6e3" "#fdf6e3")))
 '(hl-paren-colors (quote ("#2aa198" "#b58900" "#268bd2" "#6c71c4" "#859900")))
 '(inf-clojure-lein-cmd "lein trampoline run -m clojure.main repl.clj")
 '(ivy-mode t)
 '(jdecomp-decompiler-options
   (quote
    ((cfr "--comments false" "--removeboilerplate false")
     (fernflower "-hes=0" "-hdc=0"))))
 '(jdecomp-decompiler-paths
   (quote
    ((procyon . "c:\\dusan\\Decompiler\\procyon-decompiler-0.5.30.jar"))))
 '(jdecomp-decompiler-type (quote procyon))
 '(linum-format " %6d ")
 '(magit-diff-use-overlays nil)
 '(main-line-color1 "#222232")
 '(main-line-color2 "#333343")
 '(main-line-separator-style (quote chamfer))
 '(nrepl-message-colors
   (quote
    ("#dc322f" "#cb4b16" "#b58900" "#546E00" "#B4C342" "#00629D" "#2aa198" "#d33682" "#6c71c4")))
 '(package-selected-packages
   (quote
    (rjsx-mode magit org-plus-contrib expand-region counsel swiper ivy company flycheck flycheck-clojure lsp-java w3 w3m cider markdown-mode pdf-tools alect-themes ample-zen-theme js2-refactor xref-js2 fireplace mc-extras multiple-cursors ido-at-point ido-vertical-mode graphql-mode tron-theme sunrise-x-w32-addons sunrise-x-tabs sunrise-x-modeline sunrise-x-mirror sunrise-x-buttons sql-indent slime-repl prettier-js powershell phoenix-dark-mono-theme pastels-on-dark-theme paredit openwith occidental-theme nzenburn-theme ntcmd noctilux-theme naquadah-theme mustang-theme monokai-theme molokai-theme moe-theme light-soap-theme late-night-theme jdecomp javap-mode ir-black-theme inkpot-theme inf-clojure image-dired+ icicles hydandata-light-theme heroku-theme hemisu-theme helm-themes gruber-darker-theme grandshell-theme gnugo github-theme ghci-completion ghc gandalf-theme flymake-gjshint flymake-css esxml espresso-theme egg dot-mode django-theme dired+ dircmp deep-thought-theme cyberpunk-theme clues-theme clojurescript-mode clojure-mode-extra-font-locking birds-of-paradise-plus-theme base16-theme auto-complete-nxml assemblage-theme abl-mode)))
 '(pos-tip-background-color "#eee8d5")
 '(pos-tip-foreground-color "#586e75")
 '(powerline-color1 "#222232")
 '(powerline-color2 "#333343")
 '(smartrep-mode-line-active-bg (solarized-color-blend "#859900" "#eee8d5" 0.2))
 '(term-default-bg-color "#fdf6e3")
 '(term-default-fg-color "#657b83")
 '(vc-annotate-background "#d4d4d4")
 '(vc-annotate-background-mode nil)
 '(vc-annotate-color-map
   (quote
    ((20 . "#437c7c")
     (40 . "#336c6c")
     (60 . "#205070")
     (80 . "#2f4070")
     (100 . "#1f3060")
     (120 . "#0f2050")
     (140 . "#a080a0")
     (160 . "#806080")
     (180 . "#704d70")
     (200 . "#603a60")
     (220 . "#502750")
     (240 . "#401440")
     (260 . "#6c1f1c")
     (280 . "#935f5c")
     (300 . "#834744")
     (320 . "#732f2c")
     (340 . "#6b400c")
     (360 . "#23733c"))))
 '(vc-annotate-very-old-color "#23733c")
 '(weechat-color-list
   (quote
    (unspecified "#fdf6e3" "#eee8d5" "#990A1B" "#dc322f" "#546E00" "#859900" "#7B6000" "#b58900" "#00629D" "#268bd2" "#93115C" "#d33682" "#00736F" "#2aa198" "#657b83" "#839496")))
 '(xterm-color-names
   ["#eee8d5" "#dc322f" "#859900" "#b58900" "#268bd2" "#d33682" "#2aa198" "#073642"])
 '(xterm-color-names-bright
   ["#fdf6e3" "#cb4b16" "#93a1a1" "#839496" "#657b83" "#6c71c4" "#586e75" "#002b36"]))

					;(add-hook 'js2-mode-hook 'prettier-js-mode)
					;(add-hook 'web-mode-hook 'prettier-js-mode)


(defun prettier-format ()
  (call-process "prettier.cmd" nil "*prettier-output*" nil "--write" (buffer-file-name))
  (revert-buffer nil 'no-confirm t))

(defun run-prettier-on-save ()
  "Format current file if it's running tide mode"
  (interactive)
  (prettier-format)
  )

;;(add-hook 'after-save-hook 'run-prettier-on-save)

(global-set-key (kbd "C-x p") 'run-prettier-on-save)

(require 'ido)
;;(ido-mode t)
(setq ido-default-buffer-method 'selected-window)

(global-set-key (kbd "C-x g") 'magit-status)

(global-set-key (kbd "C-c m c") 'mc/edit-lines)

(customize-set-variable 'jdecomp-decompiler-type 'procyon)

(customize-set-variable 'jdecomp-decompiler-paths
                        '((procyon . "c:\\dusan\\Decompiler\\procyon-decompiler-0.5.30.jar")))

(setq tramp-default-method "plink")

(global-set-key (kbd "C-S-c C-S-c") 'mc/edit-lines)

(add-to-list 'auto-mode-alist '("\\.js\\'" . rjsx-mode))


(add-hook 'rjsx-mode-hook (lambda ()
			    (add-hook 'xref-backend-functions #'xref-js2-xref-backend nil t)))

(add-hook 'js2-mode-hook (lambda ()
  (add-hook 'xref-backend-functions #'xref-js2-xref-backend nil t)))

(require 'multiple-cursors)

(global-set-key (kbd "C-S-c C-S-c") 'mc/edit-lines)

(global-set-key (kbd "C->") 'mc/mark-next-like-this)
(global-set-key (kbd "C-S->") 'mc/mark-next-word-like-this)
(global-set-key (kbd "C-<") 'mc/mark-previous-like-this)
(global-set-key (kbd "C-S-<") 'mc/mark-previous-like-this)
(global-set-key (kbd "C-c C-<") 'mc/mark-all-like-this)
(global-set-key (kbd "C-?") 'mc/mark-all-like-this-dwim)
(global-set-key (kbd "C-!") 'mc/insert-numbers)
(global-set-key (kbd "C-#") 'mc/sort-regions)


(global-unset-key (kbd "M-<down-mouse-1>"))
(global-set-key (kbd "M-<mouse-1>") 'mc/add-cursor-on-click)

;;(add-hook 'java-mode-hook #'lsp)
;;(add-hook 'java-mode-hook 'flycheck-mode)

(require 'swiper)
(ivy-mode 1)
(global-set-key "\C-s" 'swiper)
(put 'upcase-region 'disabled nil)
(put 'downcase-region 'disabled nil)

(require 'expand-region)
(global-set-key (kbd "C-=") 'er/expand-region)

(defun increment-number-at-point ()
      (interactive)
      (skip-chars-backward "0-9")
      (or (looking-at "[0-9]+")
          (error "No number at point"))
      (replace-match (number-to-string (1+ (string-to-number (match-string 0))))))

(global-set-key (kbd "C-c +") 'increment-number-at-point)

(define-key global-map "\C-cl" 'org-store-link)
(define-key global-map "\C-ca" 'org-agenda)
(setq org-log-done t)
