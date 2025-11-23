INSERT INTO {tempTableName} {tableColumnNames}
SELECT 
    , MIN("testid")
	, MIN("time") + ((MAX("time") - MIN("time"))/2) AS "time"
    , {namesWithoutTimeOrGranularity}
    , ? AS "granularity"
    {valuesAggregation}
FROM {originalTableName}
WHERE 
	"time" >= ? 
AND "time" < ? 
AND "granularity" < ?
GROUP BY "testid", {namesWithoutTimeOrGranularity}
;