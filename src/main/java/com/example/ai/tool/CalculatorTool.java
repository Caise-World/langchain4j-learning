package com.example.ai.tool;

import dev.langchain4j.agent.tool.Tool;
import org.springframework.stereotype.Component;

@Component
public class CalculatorTool {

    @Tool("Calculate a mathematical expression")
    public double calculate(String expression) {
        // Remove all whitespace
        expression = expression.replaceAll("\\s+", "");

        // Only allow safe characters for calculation
        if (!expression.matches("[0-9+\\-*/().]+")) {
            throw new IllegalArgumentException("Invalid expression: " + expression);
        }

        return evaluateExpression(expression);
    }

    private double evaluateExpression(String expression) {
        return new ExpressionEvaluator(expression).evaluate();
    }

    // Simple recursive descent parser for basic arithmetic
    private static class ExpressionEvaluator {
        private final String expr;
        private int pos = 0;

        ExpressionEvaluator(String expr) {
            this.expr = expr;
        }

        double evaluate() {
            double result = parseExpression();
            if (pos < expr.length()) {
                throw new IllegalArgumentException("Unexpected character: " + expr.charAt(pos));
            }
            return result;
        }

        private double parseExpression() {
            double result = parseTerm();
            while (pos < expr.length()) {
                char op = expr.charAt(pos);
                if (op != '+' && op != '-') break;
                pos++;
                double right = parseTerm();
                result = (op == '+') ? result + right : result - right;
            }
            return result;
        }

        private double parseTerm() {
            double result = parseFactor();
            while (pos < expr.length()) {
                char op = expr.charAt(pos);
                if (op != '*' && op != '/') break;
                pos++;
                double right = parseFactor();
                if (op == '/') {
                    if (right == 0) throw new ArithmeticException("Division by zero");
                    result = result / right;
                } else {
                    result = result * right;
                }
            }
            return result;
        }

        private double parseFactor() {
            if (pos < expr.length() && expr.charAt(pos) == '(') {
                pos++;
                double result = parseExpression();
                if (pos >= expr.length() || expr.charAt(pos) != ')') {
                    throw new IllegalArgumentException("Missing closing parenthesis");
                }
                pos++;
                return result;
            }
            return parseNumber();
        }

        private double parseNumber() {
            StringBuilder sb = new StringBuilder();
            while (pos < expr.length() && (Character.isDigit(expr.charAt(pos)) || expr.charAt(pos) == '.')) {
                sb.append(expr.charAt(pos));
                pos++;
            }
            if (sb.length() == 0) {
                throw new IllegalArgumentException("Expected number at position " + pos);
            }
            return Double.parseDouble(sb.toString());
        }
    }
}