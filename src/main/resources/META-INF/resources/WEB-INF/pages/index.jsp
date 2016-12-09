<!DOCTYPE html>
<html lang="en">
<head>
    <%@ taglib prefix="jstl" uri="http://java.sun.com/jsp/jstl/core" %>
    <%@ page contentType="text/html;charset=UTF-8" language="java" %>
    <meta charset="utf-8">
    <meta http-equiv="X-UA-Compatible" content="IE=edge">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <script src="recourses/jquery.min.js"></script>
    <script src="recourses/bootstrap.min.js"></script>
    <script src="recourses/datepicher.min.js"></script>
    <link href="recourses/bootstrap.min.css" rel="stylesheet" type="text/css">
    <link href="recourses/application.css" rel="stylesheet" type="text/css">
    <link href="recourses/bootstrap-dialog.min.css" rel="stylesheet" type="text/css">
    <script src="recourses/bootstrap-dialog.min.js"></script>
</head>
<body>

<div class="container">
    <div class="row">
        <jstl:set var="count" value="0" scope="page"/>
        <table>
            <tr>
                <td><h4>UpdateTime:</h4></td>
                <td><h4>${TimeStamp}</h4></td>
            </tr>
            <tr>
                <td><h4>Last agent statistic clear:</h4></td>
                <td><h4> ${LastCleared}</h4></td>
            </tr>
        </table>
        <h4>Pool Statistic</h4>
        <table border="1">
            <thead>
            <tr>
                <td>#</td>
                <td>poolName</td>
                <td>size</td>
            </tr>
            </thead>
            <tbody>
            <tr>
                <td>1</td>
                <td>AgentMapping</td>
                <td>${AgentMapping}</td>
            </tr>
            <tr>
                <td>2</td>
                <td>InstrumentMapping</td>
                <td>${InstrumentMapping}</td>
            </tr>
            <tr>
                <td>3</td>
                <td>Subscribers</td>
                <td>${Subscribers}</td>
            </tr>
            <tr>
                <td>4</td>
                <td>CallsHolder</td>
                <td>${CallsHolder}</td>
            </tr>
            <tr>
                <td>5</td>
                <td>MonitorsHolder</td>
                <td>${MonitorsHolder}</td>
            </tr>
            </tbody>
        </table>
        <form action="/cti-emulator/web/agents_stat_clear">
            <table>
                <tr>
                    <td><h4>Agents Statistic</h4></td>
                    <td><input type="submit" value="clear statistic"></td>
                </tr>
            </table>
        </form>
        <table border="1">
            <thead>
            <tr>
                <td>#</td>
                <td>AgentId</td>
                <td>State statistic</td>
                <td>sum</td>
                <td>Call statistic</td>
                <td>sum</td>
                <td>Statistic ID</td>
            </tr>
            </thead>
            <tbody>
            <jstl:forEach var="statistic" items="${statistic}">
                <jstl:set var="count" value="${count + 1}" scope="page"/>
                <tr>
                    <td><jstl:out value="${count}"/></td>
                    <td>${statistic[0]}</td>
                    <td>${statistic[1]}</td>
                    <td>${statistic[2]}</td>
                    <td>${statistic[3]}</td>
                    <td>${statistic[4]}</td>
                    <td>${statistic[5]}</td>
                </tr>
            </jstl:forEach>
            </tbody>
        </table>
    </div>
</div>
</body>
</html>