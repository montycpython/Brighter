
package com.supernatural.bwriter.web

import kotlin.js.JsString
import kotlin.js.js
import kotlin.js.toJsString

// Hand-written interop to KaTeX, the browser LaTeX renderer. index.html loads
// KaTeX from the jsDelivr CDN and exposes renderToString as
// window.BwriterKatex before this module starts.
//
// The shared parser (BookMarkup in :shared) cuts book text into segments,
// including InlineMath ($...$) and DisplayMath ($$...$$). Android renders
// those segments with JLatexMath; the web build renders them here, from the
// same segment content, so one book shows the same formulas on both.
//
// The function below is a js() snippet: the compiler places the code string
// into a JavaScript function whose parameters carry the same names, so the
// snippet reads its arguments directly.

private fun katexRenderToString(tex: JsString, displayMode: Boolean): JsString =
  js("window.BwriterKatex.renderToString(tex, displayMode)")

object MathRender {

  /** Renders one LaTeX formula to an HTML string. Display mode for $$...$$. */
  fun render(tex: String, displayMode: Boolean): String =
    katexRenderToString(tex.toJsString(), displayMode).toString()
}
